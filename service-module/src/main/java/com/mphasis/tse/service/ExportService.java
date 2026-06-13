package com.mphasis.tse.service;

import com.mphasis.tse.dto.TradeExportProjection;
import com.mphasis.tse.repository.TradeArchiveRepository;
import com.mphasis.tse.repository.TransactionMainTableRepository;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final TransactionMainTableRepository mainRepo;
    private final TradeArchiveRepository archiveRepo;
    private final TransactionMetaTableRepository metaRepo;
    private final com.mphasis.tse.repository.ExportJobRepository exportJobRepository;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public void streamActiveTransactions(String startDate, String endDate, Long fileId, Optional<Long> userId, Consumer<Stream<TradeExportProjection>> streamConsumer) {
        Stream<TradeExportProjection> stream;
        
        if (fileId != null) {
            stream = userId.map(u -> mainRepo.streamByFileIdAndUserId(fileId, u))
                           .orElseGet(() -> mainRepo.streamByFileId(fileId));
        } else {
            if (userId.isPresent()) {
                stream = mainRepo.streamByFileHeaderDateBetweenAndUserId(startDate, endDate, userId.get());
            } else {
                stream = mainRepo.streamByFileHeaderDateBetween(startDate, endDate);
            }
        }

        try (stream) {
            streamConsumer.accept(stream);
        }
    }

    @Transactional(readOnly = true)
    public void streamArchivedTransactions(String startDate, String endDate, Long fileId, Optional<Long> userId, Consumer<Stream<TradeExportProjection>> streamConsumer) {
        Stream<TradeExportProjection> stream;
        
        if (fileId != null) {
            stream = archiveRepo.streamByFileId(fileId);
        } else {
            if (userId.isPresent()) {
                List<Long> fileIds = metaRepo.findActiveFileIdsByUserId(userId.get());
                if (!fileIds.isEmpty()) {
                    stream = archiveRepo.streamByFileIdInAndFileHeaderDateBetween(fileIds, startDate, endDate);
                } else {
                    stream = Stream.empty();
                }
            } else {
                stream = archiveRepo.streamByFileHeaderDateBetween(startDate, endDate);
            }
        }

        try (stream) {
            streamConsumer.accept(stream);
        }
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupExpiredExports() {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusHours(24);
        List<com.mphasis.tse.entity.ExportJob> expiredJobs = exportJobRepository.findByDownloadedTrueAndDownloadedAtBefore(cutoff);
        
        for (com.mphasis.tse.entity.ExportJob job : expiredJobs) {
            if (job.getS3Url() != null) {
                try {
                    s3Service.deleteFile(job.getS3Url());
                } catch (Exception e) {
                    // Log error and continue with other deletions
                    System.err.println("Failed to delete from S3: " + job.getS3Url() + " - " + e.getMessage());
                }
            }
            exportJobRepository.delete(job);
        }
    }
}
