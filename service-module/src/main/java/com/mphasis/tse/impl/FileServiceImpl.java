package com.mphasis.tse.impl;

import com.mphasis.tse.dto.*;
import com.mphasis.tse.entity.FileLoadMetaData;
import com.mphasis.tse.entity.TransactionError;
import com.mphasis.tse.entity.User;
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
import com.mphasis.tse.repository.DeletedTradeTransactionRepository;
import com.mphasis.tse.repository.DeletedTransactionErrorRepository;
import com.mphasis.tse.repository.UserRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final TransactionErrorMapper transactionErrorMapper;
    private final FileLoadMetaDataMapper fileLoadMetaDataMapper;
    private final TransactionMetaTableRepository repository;
    private final String tempDir;

    private final TransactionMainTableRepository transactionMainTableRepository;

    private final TradeArchiveRepository tradeArchiveRepository;

    private final DeletedTradeTransactionRepository deletedTradeTransactionRepository;
    private final DeletedTransactionErrorRepository deletedTransactionErrorRepository;
    private final UserRepository userRepository;

    public FileServiceImpl(TransactionMetaTableRepository transactionMetaTableRepository, TransactionErrorRepository transactionErrorRepository, AsyncProcessingService asyncProcessingService, TransactionMainTableRepository transactionMainTableRepository, TradeArchiveRepository tradeArchiveRepository,
                           DeletedTradeTransactionRepository deletedTradeTransactionRepository, DeletedTransactionErrorRepository deletedTransactionErrorRepository,
                           UserRepository userRepository,
                           @Qualifier("tradeFileProcessingJob") Job job, TransactionErrorMapper transactionErrorMapper, FileLoadMetaDataMapper fileLoadMetaDataMapper, TransactionMetaTableRepository repository,@Value("${file.upload.temp-dir}") String tempDir) {

        this.transactionMetaTableRepository = transactionMetaTableRepository;
        this.transactionErrorRepository = transactionErrorRepository;
        this.transactionMainTableRepository = transactionMainTableRepository;
        this.tradeArchiveRepository = tradeArchiveRepository;
        this.deletedTradeTransactionRepository = deletedTradeTransactionRepository;
        this.deletedTransactionErrorRepository = deletedTransactionErrorRepository;
        this.userRepository = userRepository;
        this.transactionErrorMapper = transactionErrorMapper;
        this.fileLoadMetaDataMapper = fileLoadMetaDataMapper;
        this.repository = repository;
        this.tempDir = tempDir;
    }

    @Override
    public List<TransactionErrorResponse> getAllErrors() {
        log.debug("Fetching all transaction errors");
        java.util.Optional<Long> userId = currentUserId();
        List<TransactionError> errors = userId.isPresent()
                ? transactionErrorRepository.findAll(ownedErrorSpec())
                : transactionErrorRepository.findAll();
        return transactionErrorMapper.toDtoList(errors);
    }

    @Override
    public PageResponse<TransactionErrorResponse> getAllErrorsPage(int page, int size) {
        var pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "createdTime"));
        return PageResponse.from(
                transactionErrorRepository.findAll(ownedErrorSpec(), pageable)
                        .map(transactionErrorMapper::toDto)
        );
    }

    @Override
    public List<FileLoadMetaDataResponse> searchFileLoads(FileSearchRequest request) {
        Specification<FileLoadMetaData> spec = ownedFileSpec().and(
                (root, query, cb) -> cb.or(cb.isNull(root.get("isDeleted")), cb.equal(root.get("isDeleted"), false))
        );
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
    public PageResponse<FileLoadMetaDataResponse> searchFileLoadsPage(FileSearchRequest request) {
        Specification<FileLoadMetaData> spec = buildFileSearchSpec(request);
        var pageable = PageRequest.of(
                normalizePage(request.getPage()),
                normalizeSize(request.getSize()),
                Sort.by(Sort.Direction.DESC, "uploadTime")
        );
        return PageResponse.from(
                transactionMetaTableRepository.findAll(spec, pageable)
                        .map(fileLoadMetaDataMapper::toResponse)
        );
    }

    @Override
    public DashboardMetricsResponse getMetrics() {
        log.debug("Fetching dashboard summary metrics");

        java.util.Optional<Long> userId = currentUserId();
        long totalFiles;
        long successRecords;
        long errorRecords;

        if (userId.isPresent()) {
            List<Long> fileIds = transactionMetaTableRepository.findActiveFileIdsByUserId(userId.get());
            totalFiles = transactionMetaTableRepository.countActiveFilesByUserId(userId.get());
            successRecords = transactionMainTableRepository.countByUserId(userId.get())
                    + (fileIds.isEmpty() ? 0 : tradeArchiveRepository.countByFileIdIn(fileIds));
            errorRecords = transactionErrorRepository.countDistinctByStatusAndUserId(ErrorStatus.FAILED, userId.get());
        } else {
            totalFiles = transactionMetaTableRepository.countActiveFiles();
            successRecords = transactionMainTableRepository.count() + tradeArchiveRepository.count();
            errorRecords = transactionErrorRepository.countDistinctByStatus(ErrorStatus.FAILED);
        }

        return new DashboardMetricsResponse(totalFiles, successRecords, errorRecords);
    }

    @Override
    public List<TransactionErrorResponse> searchFileErrors(TransactionErrorSearchRequest request) {
        log.info("Search File Errors started");
        Specification<TransactionError> spec = buildErrorSearchSpec(request);
        List<TransactionError> result = transactionErrorRepository.findAll(spec);
        return transactionErrorMapper.toDtoList(result);
    }

    @Override
    public PageResponse<TransactionErrorResponse> searchFileErrorsPage(TransactionErrorSearchRequest request) {
        Specification<TransactionError> spec = buildErrorSearchSpec(request);
        var pageable = PageRequest.of(
                normalizePage(request.getPage()),
                normalizeSize(request.getSize()),
                Sort.by(Sort.Direction.DESC, "createdTime")
        );
        return PageResponse.from(
                transactionErrorRepository.findAll(spec, pageable)
                        .map(transactionErrorMapper::toDto)
        );
    }


    @Override
    public FileLoadMetaDataResponse modifyFileLoadStatus(FileLoadMetaData request) {
        log.info("Modify started for id={} with newStatus={}",
                request.getFileId(), request.getStatus());
        FileLoadMetaData entity = findAccessibleFileLoad(request.getFileId());
        entity.setStatus(request.getStatus());
        FileLoadMetaData saved = transactionMetaTableRepository.save(entity);
        log.info("Modify completed for id={} finalStatus={}",
                saved.getFileId(), saved.getStatus());
        return fileLoadMetaDataMapper.toResponse(entity);
    }


    @Override
    @Transactional
    public void deleteFileLoad(Long id) {
        log.info("Delete File Load started for id: {}", id);

        FileLoadMetaData metaData = findAccessibleFileLoad(id);

        LocalDateTime deletedAt = LocalDateTime.now();

        // 1. Move related active transactions to deleted table
        deletedTradeTransactionRepository.moveByFileId(id, deletedAt);

        // 2. Move related archived transactions to deleted table
        deletedTradeTransactionRepository.moveFromArchiveByFileId(id, deletedAt);

        // 3. Move transaction errors to deleted table
        deletedTransactionErrorRepository.moveByFileId(id, deletedAt);

        // 4. Delete original trade transactions
        transactionMainTableRepository.deleteByFileId(id);

        // 5. Delete original trade archive records
        tradeArchiveRepository.deleteByFileId(id);

        // 6. Delete original transaction errors
        transactionErrorRepository.deleteByFileId(id);

        // 7. Update file load metadata
        metaData.setStatus(FileStatus.DELETED);
        metaData.setIsDeleted(true);
        metaData.setDeletedAt(deletedAt);
        transactionMetaTableRepository.save(metaData);

        // duplicate block removed
        log.info("Delete File Load completed for id: {}", id);
    }



    @Override
    @Transactional
    public void archiveFileLoad(Long fileId) {

        log.info("Archive File Load started for id: {}", fileId);

        FileLoadMetaData metaData = findAccessibleFileLoad(fileId);

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

            FileLoadMetaData metaData = saveInitialMetaData(file);

            String filePath = saveFileToDisk(file, metaData.getFileId());
            
            metaData.setFilePath(filePath);
            metaData.setTotalRecords(0); // Set to 0 initially; counted asynchronously by JobListener before processing starts
            transactionMetaTableRepository.save(metaData);

            log.info("File {} uploaded successfully and saved at {}", file.getOriginalFilename(), filePath);

            return new FileUploadResponse(
                    metaData.getFileId(),
                    metaData.getFilename(),
                    FileStatus.PENDING.name(),
                    "Queued for processing"
            );

        } catch (Exception e) {
            log.error("File upload failed", e);
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }

    @Override
    public List<FileLoadMetaData> getAllFileLoads() {

        log.info("Fetching all file loads");

        List<FileLoadMetaData> files = currentUserId().isPresent()
                ? repository.findAll(ownedFileSpec())
                : repository.findAll();

        return files.stream()
                .filter(file -> file.getStatus() != FileStatus.DELETED
                        && (file.getIsDeleted() == null || !file.getIsDeleted())) // only ACTIVE and ARCHIVED
                .toList();
    }

    @Override
    public PageResponse<FileLoadMetaDataResponse> getAllFileLoadsPage(int page, int size) {
        Specification<FileLoadMetaData> spec = ownedFileSpec().and(
                (root, query, cb) -> cb.and(
                        cb.notEqual(root.get("status"), FileStatus.DELETED),
                        cb.or(cb.isNull(root.get("isDeleted")), cb.equal(root.get("isDeleted"), false))
                )
        );
        var pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "uploadTime"));
        return PageResponse.from(
                transactionMetaTableRepository.findAll(spec, pageable)
                        .map(fileLoadMetaDataMapper::toResponse)
        );
    }

    @Override
    @Transactional
    public void resolveErrorManual(Long errorId) {
        log.info("Manually resolving errorId: {}", errorId);
        TransactionError error = transactionErrorRepository.findById(errorId)
                .orElseThrow(() -> new RuntimeException("Error record not found for id " + errorId));
        assertErrorAccess(error);
        if (error.getMetaData() != null && error.getMetaData().getStatus() == FileStatus.ARCHIVED) {
            throw new RuntimeException("Cannot modify errors of an archived file");
        }
        error.setStatus(ErrorStatus.RESOLVED);
        transactionErrorRepository.save(error);

        if (error.getMetaData() != null) {
            recalculateFileCompletion(error.getMetaData().getFileId());
        }
    }

    @Override
    @Transactional
    public void ignoreErrorManual(Long errorId) {
        log.info("Manually ignoring errorId: {}", errorId);
        TransactionError error = transactionErrorRepository.findById(errorId)
                .orElseThrow(() -> new RuntimeException("Error record not found for id " + errorId));
        assertErrorAccess(error);
        if (error.getMetaData() != null && error.getMetaData().getStatus() == FileStatus.ARCHIVED) {
            throw new RuntimeException("Cannot modify errors of an archived file");
        }
        if (error.getStatus() != ErrorStatus.INVALID_TRANSACTION_ID) {
            throw new RuntimeException("Only invalid transaction ID errors can be ignored manually");
        }
        error.setStatus(ErrorStatus.IGNORED);
        transactionErrorRepository.save(error);

        if (error.getMetaData() != null) {
            recalculateFileCompletion(error.getMetaData().getFileId());
        }
    }

    @Override
    @Transactional
    public void recalculateFileCompletion(Long fileId) {
        log.info("Recalculating completion for fileId: {}", fileId);
        FileLoadMetaData meta = transactionMetaTableRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("File metadata not found for id " + fileId));

        long activeErrors = transactionErrorRepository.countByMetaData_FileIdAndStatusIn(
                fileId,
                List.of(ErrorStatus.FAILED, ErrorStatus.INVALID_TRANSACTION_ID)
        );
        if (activeErrors > 0) {
            meta.setStatus(FileStatus.COMPLETED_WITH_ERROR);
        } else {
            meta.setStatus(FileStatus.COMPLETED);
        }
        transactionMetaTableRepository.save(meta);
        log.info("Updated fileId={} status to {}", fileId, meta.getStatus());
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

    private String saveFileToDisk(MultipartFile file, Long fileId) throws Exception {
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String filename = fileId + "_" + originalFilename;

        Path uploadPath = Paths.get(tempDir);
        Files.createDirectories(uploadPath);

        Path filePath = uploadPath.resolve(filename).normalize();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("File saved to: {}", filePath);
        return filePath.toString();

    }

    private FileLoadMetaData saveInitialMetaData(MultipartFile file) {
        FileLoadMetaData metaData = new FileLoadMetaData();
        metaData.setFilename(file.getOriginalFilename());
        metaData.setUploadTime(LocalDateTime.now());
        metaData.setStatus(FileStatus.PENDING);
        metaData.setTotalRecords(0);
        metaData.setSuccessCount(0);
        metaData.setErrorCount(0);
        metaData.setDuplicateCount(0);
        currentUser().ifPresent(metaData::setUser);
        return transactionMetaTableRepository.save(metaData);
    }

    private java.util.Optional<User> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return java.util.Optional.empty();
        }
        return userRepository.findByEmail(authentication.getName());
    }

    private java.util.Optional<Long> currentUserId() {
        return currentUser().map(User::getId);
    }

    private Specification<FileLoadMetaData> ownedFileSpec() {
        return currentUserId()
                .<Specification<FileLoadMetaData>>map(FileLoadSpecification::belongsToUser)
                .orElse(Specification.unrestricted());
    }

    private Specification<TransactionError> ownedErrorSpec() {
        return currentUserId()
                .<Specification<TransactionError>>map(TransactionErrorSpecification::belongsToUser)
                .orElse(Specification.unrestricted());
    }

    private FileLoadMetaData findAccessibleFileLoad(Long fileId) {
        FileLoadMetaData metaData = transactionMetaTableRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found for id " + fileId));
        currentUserId().ifPresent(userId -> {
            Long ownerId = metaData.getUser() != null ? metaData.getUser().getId() : null;
            if (!userId.equals(ownerId)) {
                throw new AccessDeniedException("You do not have access to file id " + fileId);
            }
        });
        return metaData;
    }

    private void assertErrorAccess(TransactionError error) {
        currentUserId().ifPresent(userId -> {
            Long ownerId = error.getMetaData() != null && error.getMetaData().getUser() != null
                    ? error.getMetaData().getUser().getId()
                    : null;
            if (!userId.equals(ownerId)) {
                throw new AccessDeniedException("You do not have access to error id " + error.getErrorId());
            }
        });
    }

    private Specification<FileLoadMetaData> buildFileSearchSpec(FileSearchRequest request) {
        Specification<FileLoadMetaData> spec = ownedFileSpec().and(
                (root, query, cb) -> cb.or(cb.isNull(root.get("isDeleted")), cb.equal(root.get("isDeleted"), false))
        );
        if (request.getFileId() != null) {
            spec = spec.and(FileLoadSpecification.hasFileId(request.getFileId()));
        }
        if (request.getFilename() != null) {
            spec = spec.and(FileLoadSpecification.hasFilename(request.getFilename()));
        }
        if (request.getStatus() != null) {
            spec = spec.and(FileLoadSpecification.hasStatus(request.getStatus()));
        }
        if (request.getStartDate() != null && request.getEndDate() != null) {
            spec = spec.and(FileLoadSpecification.hasUploadTimeBetween(request.getStartDate(), request.getEndDate()));
        }
        return spec;
    }

    private Specification<TransactionError> buildErrorSearchSpec(TransactionErrorSearchRequest request) {
        Specification<TransactionError> spec = Specification.where(ownedErrorSpec());
        if (request.getFileLoadId() != null) {
            spec = spec.and(TransactionErrorSpecification.hasFileLoadId(request.getFileLoadId()));
        }
        if (request.getTransactionId() != null) {
            spec = spec.and(TransactionErrorSpecification.hasTransactionId(request.getTransactionId()));
        }
        if (request.getAccountNumber() != null) {
            spec = spec.and(TransactionErrorSpecification.hasAccountNumber(request.getAccountNumber()));
        }
        if (request.getErrorField() != null) {
            spec = spec.and(TransactionErrorSpecification.hasErrorField(request.getErrorField()));
        }
        if (request.getStatus() != null && !request.getStatus().equalsIgnoreCase("ALL")) {
            ErrorStatus statusEnum = ErrorStatus.valueOf(request.getStatus().toUpperCase());
            spec = spec.and(TransactionErrorSpecification.hasStatus(statusEnum));
        } else {
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.notEqual(root.get("status"), ErrorStatus.IGNORED),
                    cb.notEqual(root.get("status"), ErrorStatus.DUPLICATE)
            ));
        }
        if (request.getGlobalSearchTerm() != null && !request.getGlobalSearchTerm().isEmpty()) {
            spec = spec.and(TransactionErrorSpecification.hasGlobalSearchTerm(request.getGlobalSearchTerm()));
        }
        return spec;
    }

    private int normalizePage(Integer page) {
        return page == null || page < 0 ? 0 : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 20;
        }
        return Math.min(size, 100);
    }
}
