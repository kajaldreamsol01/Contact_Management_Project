package com.contactmanagement.processor;

import com.contactmanagement.dto.ContactFileResponseDto;
import com.contactmanagement.entity.Contact;
import com.contactmanagement.entity.FileMapping;
import com.contactmanagement.repository.FileMappingRepository;
import com.contactmanagement.response.ApiResponse;
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
import java.util.ArrayList;
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
        if (CollectionUtils.isEmpty(files))
            return ApiResponse.response("FAILED", "Please select at least one file");
        if (CollectionUtils.isEmpty(types) || files.size() != types.size())
            return ApiResponse.response("FAILED", "File type mapping is invalid");
        try {
            List<ContactFileResponseDto> result = IntStream.range(0, files.size())
                    .mapToObj(index -> store(files.get(index), types.get(index)))
                    .toList();
            return ApiResponse.response("SUCCESS", result.size() + " file(s) uploaded successfully", result);
        } catch (Exception exception) {
            Throwable cause = exception instanceof IllegalArgumentException ? exception : exception.getCause();
            return ApiResponse.response(
                    "FAILED",
                    cause instanceof IllegalArgumentException ? cause.getMessage() : "Unable to upload files"
            );
        }
    }

    public ResponseEntity<Resource> download(String uuid) {
        if (!StringUtils.hasText(uuid) || !uuid.matches("^[a-fA-F0-9]{32}$"))
            return ResponseEntity.badRequest().build();

        FileMapping mapping = repository.findById(uuid).orElse(null);
        if (Objects.isNull(mapping)) return ResponseEntity.notFound().build();

        try {
            String extension = Objects.toString(
                    StringUtils.getFilenameExtension(mapping.getFileName()),
                    ""
            ).toLowerCase();
            String folder = Objects.toString(mapping.getFileType(), "file").toLowerCase();
            String savedName = mapping.getUuid() + (StringUtils.hasText(extension) ? "." + extension : "");

            Path root = Paths.get(storageDirectory).toAbsolutePath().normalize();
            Path workingDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
            Path stablePath = root.resolve(folder).resolve(savedName);

            List<Path> candidates = new ArrayList<>(List.of(
                    stablePath,
                    workingDirectory.resolve("uploads").resolve(folder).resolve(savedName)
            ));

            if (StringUtils.hasText(mapping.getFilePath())) {
                try {
                    candidates.add(Paths.get(mapping.getFilePath()).toAbsolutePath().normalize());
                } catch (Exception ignored) {
                }
            }

            Path parent = workingDirectory.getParent();
            if (Objects.nonNull(parent)) {
                candidates.add(parent.resolve("uploads").resolve(folder).resolve(savedName));
                candidates.add(parent.resolve("contact-management").resolve("uploads").resolve(folder).resolve(savedName));
            }

            Path filePath = candidates.stream()
                    .filter(path -> Files.isRegularFile(path) && Files.isReadable(path))
                    .findFirst()
                    .orElse(null);
            if (Objects.isNull(filePath)) return ResponseEntity.notFound().build();

            if (!filePath.equals(stablePath)) {
                try {
                    Files.createDirectories(stablePath.getParent());
                    Files.copy(filePath, stablePath, StandardCopyOption.REPLACE_EXISTING);
                    filePath = stablePath;
                } catch (Exception ignored) {
                }
            }

            String resolvedPath = filePath.toAbsolutePath().normalize().toString();
            if (!resolvedPath.equals(mapping.getFilePath())) {
                mapping.setFilePath(resolvedPath);
                repository.save(mapping);
            }

            return ResponseEntity.ok()
                    .contentType(mediaType(extension))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(mapping.getFileName(), StandardCharsets.UTF_8)
                                    .build()
                                    .toString()
                    )
                    .contentLength(Files.size(filePath))
                    .body(new UrlResource(filePath.toUri()));
        } catch (Exception exception) {
            return ResponseEntity.notFound().build();
        }
    }

    public ApiResponse<Void> delete(String uuid) {
        if (!StringUtils.hasText(uuid) || !uuid.matches("^[a-fA-F0-9]{32}$"))
            return ApiResponse.response("FAILED", "Invalid file UUID");

        FileMapping mapping = repository.findById(uuid).orElse(null);
        if (Objects.isNull(mapping))
            return ApiResponse.response("SUCCESS", "File already removed");

        try {
            String extension = Objects.toString(
                    StringUtils.getFilenameExtension(mapping.getFileName()),
                    ""
            ).toLowerCase();
            String folder = Objects.toString(mapping.getFileType(), "file").toLowerCase();
            String savedName = mapping.getUuid() + (StringUtils.hasText(extension) ? "." + extension : "");

            Path root = Paths.get(storageDirectory).toAbsolutePath().normalize();
            Path workingDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();

            Set<Path> candidates = new LinkedHashSet<>();
            candidates.add(root.resolve(folder).resolve(savedName));
            candidates.add(workingDirectory.resolve("uploads").resolve(folder).resolve(savedName));

            if (StringUtils.hasText(mapping.getFilePath())) {
                try {
                    candidates.add(Paths.get(mapping.getFilePath()).toAbsolutePath().normalize());
                } catch (Exception ignored) {
                }
            }

            Path parent = workingDirectory.getParent();
            if (Objects.nonNull(parent)) {
                candidates.add(parent.resolve("uploads").resolve(folder).resolve(savedName));
                candidates.add(parent.resolve("contact-management").resolve("uploads").resolve(folder).resolve(savedName));
            }

            for (Path path : candidates) {
                if (Objects.nonNull(path))
                    Files.deleteIfExists(path);
            }

            repository.delete(mapping);
            repository.flush();

            return ApiResponse.response("SUCCESS", "File removed successfully");
        } catch (Exception exception) {
            return ApiResponse.response("FAILED", "Unable to remove file");
        }
    }

    public Map<String, ContactFileResponseDto> metadata(Collection<Contact> contacts) {
        if (CollectionUtils.isEmpty(contacts)) return Map.of();

        Set<String> uuids = new LinkedHashSet<>();
        contacts.forEach(contact -> {
            if (StringUtils.hasText(contact.getPhotoUuid())) uuids.add(contact.getPhotoUuid());
            if (!CollectionUtils.isEmpty(contact.getDocumentUuids()))
                contact.getDocumentUuids().stream()
                        .filter(StringUtils::hasText)
                        .forEach(uuids::add);
        });
        if (uuids.isEmpty()) return Map.of();

        Map<String, ContactFileResponseDto> result = new HashMap<>();
        repository.findAllById(uuids).forEach(file -> result.put(
                file.getUuid(),
                new ContactFileResponseDto(file.getUuid(), file.getFileName(), file.getFileType())
        ));
        return result;
    }

    public Map<String, ContactFileResponseDto> metadataByUuids(Collection<String> uuids) {
        if (CollectionUtils.isEmpty(uuids)) return Map.of();
        Set<String> ids = new LinkedHashSet<>();
        uuids.stream().filter(StringUtils::hasText).forEach(ids::add);
        if (ids.isEmpty()) return Map.of();
        Map<String, ContactFileResponseDto> result = new HashMap<>();
        repository.findAllById(ids).forEach(file -> result.put(
                file.getUuid(),
                new ContactFileResponseDto(file.getUuid(), file.getFileName(), file.getFileType())
        ));
        return result;
    }

    private ContactFileResponseDto store(MultipartFile file, String typeValue) {
        String type = Objects.toString(typeValue, "").trim().toUpperCase();
        String fileName = StringUtils.cleanPath(Objects.toString(file.getOriginalFilename(), "file"));

        if (file.isEmpty()) throw new IllegalArgumentException("Invalid or empty file");
        if (file.getSize() > MAX_FILE_SIZE)
            throw new IllegalArgumentException(fileName + " exceeds the 10 MB file limit");
        if (!List.of("PHOTO", "DOCUMENT").contains(type))
            throw new IllegalArgumentException("Invalid attachment type");
        if (!StringUtils.hasText(fileName) || fileName.contains(".."))
            throw new IllegalArgumentException("Invalid file name");

        String extension = Objects.toString(StringUtils.getFilenameExtension(fileName), "").toLowerCase();
        List<String> allowedExtensions = "PHOTO".equals(type)
                ? List.of("jpg", "jpeg", "png")
                : List.of("pdf", "doc", "docx", "xls", "xlsx");
        if (!allowedExtensions.contains(extension))
            throw new IllegalArgumentException(
                    "PHOTO".equals(type)
                            ? "Photo must be JPG, JPEG or PNG"
                            : "Document must be PDF, DOC, DOCX, XLS or XLSX"
            );

        String uuid = UUID.randomUUID().toString().replace("-", "");
        Path directory = Paths.get(storageDirectory).toAbsolutePath().normalize().resolve(type.toLowerCase());
        Path path = directory.resolve(uuid + "." + extension);

        try {
            Files.createDirectories(directory);
            try (var input = file.getInputStream()) {
                Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
            }

            FileMapping mapping = new FileMapping();
            mapping.setUuid(uuid);
            mapping.setFileName(fileName);
            mapping.setFilePath(path.toAbsolutePath().toString());
            mapping.setFileType(type);
            repository.saveAndFlush(mapping);

            return new ContactFileResponseDto(uuid, fileName, type);
        } catch (Exception exception) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception ignored) {
            }
            throw new IllegalStateException("Unable to store file", exception);
        }
    }

    private MediaType mediaType(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "pdf" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
}
