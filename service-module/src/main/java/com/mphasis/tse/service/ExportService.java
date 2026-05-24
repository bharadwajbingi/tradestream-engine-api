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
}
