package com.contactmanagement.processor;

import com.contactmanagement.common.constants.AttachmentValidation;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.dto.ContactFileResponseDto;
import com.contactmanagement.entity.Contact;
import com.contactmanagement.entity.FileMapping;
import com.contactmanagement.repository.FileMappingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class ContactFileProcessor {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private final FileMappingRepository repository;
    @Value("${contact.storage.directory}")
    private String storageDirectory;

    public ApiResponse<List<ContactFileResponseDto>> upload(List<MultipartFile> files, List<String> types) {
        if (CollectionUtils.isEmpty(files)) return ApiResponse.response("FAILED", "Please select at least one file");
        if (CollectionUtils.isEmpty(types) || files.size() != types.size())
            return ApiResponse.response("FAILED", "File type mapping is invalid");
        try {
            List<ContactFileResponseDto> uploadedFiles = IntStream.range(0, files.size()).mapToObj(index -> store(files.get(index), types.get(index))).toList();
            return ApiResponse.response("SUCCESS", uploadedFiles.size() + " file(s) uploaded successfully", uploadedFiles);
        } catch (Exception exception) {
            Throwable cause = exception instanceof IllegalArgumentException ? exception : exception.getCause();
            return ApiResponse.response("FAILED", cause instanceof IllegalArgumentException ? cause.getMessage() : "Unable to upload files");
        }
    }

    public ResponseEntity<Resource> download(String uuid) {
        if (!valid(uuid)) return ResponseEntity.badRequest().build();
        FileMapping fileMapping = repository.findById(uuid).orElse(null);
        if (Objects.isNull(fileMapping)) return ResponseEntity.notFound().build();
        try {
            FilePaths filePaths = paths(fileMapping);
            Path filePath = filePaths.candidates().stream().filter(candidatePath -> Files.isRegularFile(candidatePath) && Files.isReadable(candidatePath)).findFirst().orElse(null);
            if (Objects.isNull(filePath)) return ResponseEntity.notFound().build();
            if (!filePath.equals(filePaths.stable())) try {
                Files.createDirectories(filePaths.stable().getParent());
                Files.copy(filePath, filePaths.stable(), StandardCopyOption.REPLACE_EXISTING);
                filePath = filePaths.stable();
            } catch (Exception ignored) {
            }
            String resolvedPath = filePath.toAbsolutePath().normalize().toString();
            if (!resolvedPath.equals(fileMapping.getFilePath())) {
                fileMapping.setFilePath(resolvedPath);
                repository.save(fileMapping);
            }
            String fileExtension = extension(fileMapping.getFileName());
            return ResponseEntity.ok().contentType(mediaType(fileExtension)).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileMapping.getFileName(), StandardCharsets.UTF_8).build().toString()).contentLength(Files.size(filePath)).body(new UrlResource(filePath.toUri()));
        } catch (Exception exception) {
            return ResponseEntity.notFound().build();
        }
    }

    public ApiResponse<Void> delete(String uuid) {
        if (!valid(uuid)) return ApiResponse.response("FAILED", "Invalid file UUID");
        FileMapping fileMapping = repository.findById(uuid).orElse(null);
        if (Objects.isNull(fileMapping)) return ApiResponse.response("SUCCESS", "File already removed");
        try {
            for (Path filePath : paths(fileMapping).candidates()) Files.deleteIfExists(filePath);
            repository.delete(fileMapping);
            repository.flush();
            return ApiResponse.response("SUCCESS", "File removed successfully");
        } catch (Exception exception) {
            return ApiResponse.response("FAILED", "Unable to remove file");
        }
    }

    public Map<String, ContactFileResponseDto> metadata(Collection<Contact> contacts) {
        if (CollectionUtils.isEmpty(contacts)) return Map.of();
        Set<String> fileUuids = new LinkedHashSet<>();
        contacts.forEach(contact -> {
            if (StringUtils.hasText(contact.getPhotoUuid())) fileUuids.add(contact.getPhotoUuid());
            if (!CollectionUtils.isEmpty(contact.getDocumentUuids()))
                contact.getDocumentUuids().stream().filter(StringUtils::hasText).forEach(fileUuids::add);
        });
        return metadataByUuids(fileUuids);
    }

    public Map<String, ContactFileResponseDto> metadataByUuids(Collection<String> uuids) {
        if (CollectionUtils.isEmpty(uuids)) return Map.of();
        Set<String> fileUuids = new LinkedHashSet<>();
        uuids.stream().filter(StringUtils::hasText).forEach(fileUuids::add);
        if (fileUuids.isEmpty()) return Map.of();
        Map<String, ContactFileResponseDto> metadata = new HashMap<>();
        repository.findAllById(fileUuids).forEach(fileMapping -> metadata.put(fileMapping.getUuid(), new ContactFileResponseDto(fileMapping.getUuid(), fileMapping.getFileName(), fileMapping.getFileType())));
        return metadata;
    }

    private ContactFileResponseDto store(MultipartFile file, String typeValue) {
        String fileType = Objects.toString(typeValue, "").trim().toUpperCase();
        String fileName = StringUtils.cleanPath(Objects.toString(file.getOriginalFilename(), "file"));
        if (!List.of("PHOTO", "DOCUMENT").contains(fileType))
            throw new IllegalArgumentException("Invalid attachment type");
        if (!StringUtils.hasText(fileName) || fileName.contains(".."))
            throw new IllegalArgumentException("Invalid file name");
        AttachmentValidation.validate(file, "PHOTO".equals(fileType) ? List.of("jpg", "jpeg", "png") : List.of("pdf", "doc", "docx", "xls", "xlsx"), MAX_FILE_SIZE, fileName + " exceeds the 10 MB file limit", "PHOTO".equals(fileType) ? "Photo must be JPG, JPEG or PNG" : "Document must be PDF, DOC, DOCX, XLS or XLSX");
        String fileExtension = extension(fileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        Path directory = Paths.get(storageDirectory).toAbsolutePath().normalize().resolve(fileType.toLowerCase());
        Path filePath = directory.resolve(uuid + "." + fileExtension);
        try {
            Files.createDirectories(directory);
            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }
            FileMapping fileMapping = new FileMapping();
            fileMapping.setUuid(uuid);
            fileMapping.setFileName(fileName);
            fileMapping.setFilePath(filePath.toAbsolutePath().toString());
            fileMapping.setFileType(fileType);
            repository.saveAndFlush(fileMapping);
            return new ContactFileResponseDto(uuid, fileName, fileType);
        } catch (Exception exception) {
            try {
                Files.deleteIfExists(filePath);
            } catch (Exception ignored) {
            }
            throw new IllegalStateException("Unable to store file", exception);
        }
    }

    private FilePaths paths(FileMapping fileMapping) {
        String fileExtension = extension(fileMapping.getFileName());
        String folderName = Objects.toString(fileMapping.getFileType(), "file").toLowerCase();
        String storedFileName = fileMapping.getUuid() + (StringUtils.hasText(fileExtension) ? "." + fileExtension : "");
        Path storageRoot = Paths.get(storageDirectory).toAbsolutePath().normalize();
        Path workingDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path stablePath = storageRoot.resolve(folderName).resolve(storedFileName);
        Set<Path> candidatePaths = new LinkedHashSet<>(List.of(stablePath, workingDirectory.resolve("uploads").resolve(folderName).resolve(storedFileName)));
        if (StringUtils.hasText(fileMapping.getFilePath())) try {
            candidatePaths.add(Paths.get(fileMapping.getFilePath()).toAbsolutePath().normalize());
        } catch (Exception ignored) {
        }
        Path parentDirectory = workingDirectory.getParent();
        if (Objects.nonNull(parentDirectory)) {
            candidatePaths.add(parentDirectory.resolve("uploads").resolve(folderName).resolve(storedFileName));
            candidatePaths.add(parentDirectory.resolve("contact-management").resolve("uploads").resolve(folderName).resolve(storedFileName));
        }
        return new FilePaths(stablePath, candidatePaths);
    }

    private boolean valid(String uuid) {
        return StringUtils.hasText(uuid) && uuid.matches("^[a-fA-F0-9]{32}$");
    }

    private String extension(String fileName) {
        return Objects.toString(StringUtils.getFilenameExtension(fileName), "").toLowerCase();
    }

    private MediaType mediaType(String fileExtension) {
        return switch (fileExtension) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "pdf" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private record FilePaths(Path stable, Set<Path> candidates) {
    }
}
