package com.mphasis.tse.impl;

import com.mphasis.tse.dto.*;
import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.enums.ErrorStatus;
import com.mphasis.tse.enums.FileStatus;
import com.mphasis.tse.exception.EmptyFileException;
import com.mphasis.tse.exception.FileNotFoundException;
import com.mphasis.tse.exception.InvalidFileFormatException;
import com.mphasis.tse.mapper.FileLoadMetaDataMapper;
import com.mphasis.tse.mapper.TransactionErrorMapper;
import com.mphasis.tse.repository.TradeArchiveRepository;
import com.mphasis.tse.repository.TransactionErrorRepository;
import com.mphasis.tse.repository.TransactionMainTableRepository;
import com.mphasis.tse.repository.TransactionMetaTableRepository;
import com.mphasis.tse.service.IFileService;
import com.mphasis.tse.service.async.AsyncProcessingService;
import com.mphasis.tse.specification.FileLoadSpecification;
import com.mphasis.tse.specification.TransactionErrorSpecification;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class FileServiceImpl implements IFileService {
    private final TransactionMetaTableRepository transactionMetaTableRepository;
    private final TransactionErrorRepository transactionErrorRepository;
    private final AsyncProcessingService asyncProcessingService;
    private final Job job;
    private final TransactionErrorMapper transactionErrorMapper;
    private final FileLoadMetaDataMapper fileLoadMetaDataMapper;
    private final TransactionMetaTableRepository repository;
    private final String tempDir;

    private final TransactionMainTableRepository transactionMainTableRepository;

    private final TradeArchiveRepository tradeArchiveRepository;

    public FileServiceImpl(TransactionMetaTableRepository transactionMetaTableRepository, TransactionErrorRepository transactionErrorRepository, AsyncProcessingService asyncProcessingService, TransactionMainTableRepository transactionMainTableRepository, TradeArchiveRepository tradeArchiveRepository,
                           @Qualifier("tradeFileProcessingJob") Job job, TransactionErrorMapper transactionErrorMapper, FileLoadMetaDataMapper fileLoadMetaDataMapper, TransactionMetaTableRepository repository,@Value("${file.upload.temp-dir}") String tempDir) {

        this.transactionMetaTableRepository = transactionMetaTableRepository;
        this.transactionErrorRepository = transactionErrorRepository;
        this.asyncProcessingService = asyncProcessingService;
        this.transactionMainTableRepository = transactionMainTableRepository;
        this.tradeArchiveRepository = tradeArchiveRepository;
        this.job = job;
        this.transactionErrorMapper = transactionErrorMapper;
        this.fileLoadMetaDataMapper = fileLoadMetaDataMapper;
        this.repository = repository;
        this.tempDir = tempDir;
    }

    @Override
    public List<TransactionErrorResponse> getAllErrors() {
        log.debug("Fetching all transaction errors");
        List<TransactionError> errors = transactionErrorRepository.findAll();
        return transactionErrorMapper.toDtoList(errors);
    }


    public List<FileLoadMetaDataResponse> searchFileLoads(FileSearchRequest request) {
        Specification<FileLoadMetaData> spec =
                Specification.where(Specification.unrestricted());
        if (request.getFileId() != null) {
            spec = spec.and(
                    FileLoadSpecification.hasFileId(request.getFileId())
            );
        }
        if (request.getFilename() != null) {
            spec = spec.and(
                    FileLoadSpecification.hasFilename(request.getFilename())
            );
        }
        if (request.getStatus() != null) {
            spec = spec.and(
                    FileLoadSpecification.hasStatus(request.getStatus())
            );
        }
        if (request.getStartDate() != null && request.getEndDate() != null) {
            spec = spec.and(
                    FileLoadSpecification.hasUploadTimeBetween(
                            request.getStartDate(),
                            request.getEndDate()
                    )
            );
        }
        List<FileLoadMetaData> result =transactionMetaTableRepository.findAll(spec);
        return fileLoadMetaDataMapper.toResponseList(result);
    }

    @Override
    public DashboardMetricsResponse getMetrics() {
        log.debug("Fetching dashboard summary metrics");

        long totalFiles = transactionMetaTableRepository.count();
        long successRecords = transactionMainTableRepository.count();
        long errorRecords =
                transactionErrorRepository.countDistinctByStatus(ErrorStatus.FAILED);

        return new DashboardMetricsResponse(totalFiles, successRecords, errorRecords);
    }


    public List<TransactionErrorResponse> searchFileErrors(TransactionErrorSearchRequest request) {
        Specification<TransactionError> spec =
                Specification.where(Specification.unrestricted());
        if (request.getFileLoadId() != null) {
            spec = spec.and(
                    TransactionErrorSpecification.hasFileLoadId(request.getFileLoadId())
            );
        }
        if (request.getTransactionId() != null) {
            spec = spec.and(
                    TransactionErrorSpecification.hasTransactionId(request.getTransactionId())
            );
        }
        if (request.getAccountNumber() != null) {
            spec = spec.and(
                    TransactionErrorSpecification.hasAccountNumber(request.getAccountNumber())
            );
        }
        if (request.getErrorField() != null) {
            spec = spec.and(
                    TransactionErrorSpecification.hasErrorField(request.getErrorField())
            );
        }
        if (request.getStatus() != null) {
            ErrorStatus statusEnum = ErrorStatus.valueOf(request.getStatus().toUpperCase());

            spec = spec.and(
                    TransactionErrorSpecification.hasStatus(statusEnum)
            );

        }
        List<TransactionError> result = transactionErrorRepository.findAll(spec);
        return transactionErrorMapper.toDtoList(result);
    }


    @Override
    public FileLoadMetaDataResponse modifyFileLoadStatus(FileLoadMetaData request) {
        log.info("Modify started for id={} with newStatus={}",
                request.getFileId(), request.getStatus());
        FileLoadMetaData entity = transactionMetaTableRepository.findById(request.getFileId())
                .orElseThrow(() -> new RuntimeException("File not found for id " + request.getFileId()));
        entity.setStatus(request.getStatus());
        FileLoadMetaData saved = transactionMetaTableRepository.save(entity);
        log.info("Modify completed for id={} finalStatus={}",
                saved.getFileId(), saved.getStatus());
        return fileLoadMetaDataMapper.toResponse(entity);
    }


    @Override
    public void deleteFileLoad(Long id) {
        log.info("Delete File Load started for id: {}", id);

        FileLoadMetaData metaData = transactionMetaTableRepository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found for id " + id));
        metaData.setStatus(FileStatus.DELETED);
        transactionMetaTableRepository.save(metaData);
        log.info("Delete File Load completed for id: {}", id);
    }

    @Override
    @Transactional
    public void archiveFileLoad(Long fileId) {

        log.info("Archive File Load started for id: {}", fileId);

        FileLoadMetaData metaData = transactionMetaTableRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File not found"));

        tradeArchiveRepository.archiveByFileId(fileId);

        transactionMainTableRepository.deleteByFileId(fileId);

        metaData.setStatus(FileStatus.ARCHIVED);
        transactionMetaTableRepository.save(metaData);

        log.info("Archive File Load completed for id: {}", fileId);
    }



    @Override
    public FileUploadResponse uploadFile(MultipartFile file) {

        try {
            validateFile(file);

            String filePath = saveFileToDisk(file);

            FileLoadMetaData metaData = saveMetaData(file);
            Long fileMetaDataId = metaData.getFileId();

            JobParameters jobParameters = buildJobParameters(filePath, fileMetaDataId);

            asyncProcessingService.process(job, jobParameters);

            String fileName = file.getOriginalFilename() != null
                    ? file.getOriginalFilename()
                    : "unknown-file";

            return new FileUploadResponse(
                    fileMetaDataId,
                    fileName,
                    FileStatus.STARTED.name(),
                    "Processing started"
            );

        } catch (Exception e) {
            log.error("File upload failed", e);
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }


    private void validateFile(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new EmptyFileException("No file provided. Please select a CSV file.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new InvalidFileFormatException("Invalid file type. Only CSV files are allowed.");
        }
    }


    private String saveFileToDisk(MultipartFile file) throws Exception {
        String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        Path uploadPath = Paths.get(tempDir);
        Files.createDirectories(uploadPath);

        Path filePath = uploadPath.resolve(filename).normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("File saved to: {}", filePath);
        return filePath.toString();

    }

    private FileLoadMetaData saveMetaData(MultipartFile file) {
        FileLoadMetaData metaData = new FileLoadMetaData();
        metaData.setFilename(file.getOriginalFilename());
        metaData.setUploadTime(LocalDateTime.now());
        metaData.setStatus(FileStatus.STARTED);
        return transactionMetaTableRepository.save(metaData);
    }


    private JobParameters buildJobParameters(String filePath, Long fileMetaDataId) {
        return new JobParametersBuilder()
                .addString("filePath", filePath)
                .addLong("fileMetaId", fileMetaDataId)
                .addLong("runId", System.currentTimeMillis())
                .toJobParameters();
    }


    @Override
    public List<FileLoadMetaData> getAllFileLoads() {

        log.info("Fetching all file loads");

        return repository.findAll().stream()
                .filter(file -> file.getStatus() != FileStatus.ARCHIVED && file.getStatus() != FileStatus.DELETED) //  only SUCCESS
                .toList();
    }
}