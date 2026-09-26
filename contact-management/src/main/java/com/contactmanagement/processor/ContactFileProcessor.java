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
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
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
            List<ContactFileResponseDto> data = IntStream.range(0, files.size()).mapToObj(i -> store(files.get(i), types.get(i))).toList();
            return ApiResponse.response("SUCCESS", data.size() + " file(s) uploaded successfully", data);
        } catch (Exception e) {
            Throwable cause = e instanceof IllegalArgumentException ? e : e.getCause();
            return ApiResponse.response("FAILED", cause instanceof IllegalArgumentException ? cause.getMessage() : "Unable to upload files");
        }
    }

    public ResponseEntity<Resource> download(String uuid) {
        if (!valid(uuid)) return ResponseEntity.badRequest().build();
        FileMapping mapping = repository.findById(uuid).orElse(null);
        if (mapping == null) return ResponseEntity.notFound().build();
        try {
            FilePaths paths = paths(mapping);
            Path file = paths.candidates().stream().filter(p -> Files.isRegularFile(p) && Files.isReadable(p)).findFirst().orElse(null);
            if (file == null) return ResponseEntity.notFound().build();
            if (!file.equals(paths.stable())) try {
                Files.createDirectories(paths.stable().getParent());
                Files.copy(file, paths.stable(), StandardCopyOption.REPLACE_EXISTING);
                file = paths.stable();
            } catch (Exception ignored) {
            }
            String resolved = file.toAbsolutePath().normalize().toString();
            if (!resolved.equals(mapping.getFilePath())) {
                mapping.setFilePath(resolved);
                repository.save(mapping);
            }
            String ext = extension(mapping.getFileName());
            return ResponseEntity.ok().contentType(mediaType(ext)).header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(mapping.getFileName(), StandardCharsets.UTF_8).build().toString()).contentLength(Files.size(file)).body(new UrlResource(file.toUri()));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    public ApiResponse<Void> delete(String uuid) {
        if (!valid(uuid)) return ApiResponse.response("FAILED", "Invalid file UUID");
        FileMapping mapping = repository.findById(uuid).orElse(null);
        if (mapping == null) return ApiResponse.response("SUCCESS", "File already removed");
        try {
            for (Path path : paths(mapping).candidates()) Files.deleteIfExists(path);
            repository.delete(mapping);
            repository.flush();
            return ApiResponse.response("SUCCESS", "File removed successfully");
        } catch (Exception e) {
            return ApiResponse.response("FAILED", "Unable to remove file");
        }
    }

    public Map<String, ContactFileResponseDto> metadata(Collection<Contact> contacts) {
        if (CollectionUtils.isEmpty(contacts)) return Map.of();
        Set<String> ids = new LinkedHashSet<>();
        contacts.forEach(c -> {
            if (StringUtils.hasText(c.getPhotoUuid())) ids.add(c.getPhotoUuid());
            if (!CollectionUtils.isEmpty(c.getDocumentUuids()))
                c.getDocumentUuids().stream().filter(StringUtils::hasText).forEach(ids::add);
        });
        return metadataByUuids(ids);
    }

    public Map<String, ContactFileResponseDto> metadataByUuids(Collection<String> uuids) {
        if (CollectionUtils.isEmpty(uuids)) return Map.of();
        Set<String> ids = new LinkedHashSet<>();
        uuids.stream().filter(StringUtils::hasText).forEach(ids::add);
        if (ids.isEmpty()) return Map.of();
        Map<String, ContactFileResponseDto> data = new HashMap<>();
        repository.findAllById(ids).forEach(f -> data.put(f.getUuid(), new ContactFileResponseDto(f.getUuid(), f.getFileName(), f.getFileType())));
        return data;
    }

    private ContactFileResponseDto store(MultipartFile file, String typeValue) {
        String type = Objects.toString(typeValue, "").trim().toUpperCase();
        String name = StringUtils.cleanPath(Objects.toString(file.getOriginalFilename(), "file"));
        if (!List.of("PHOTO", "DOCUMENT").contains(type)) throw new IllegalArgumentException("Invalid attachment type");
        if (!StringUtils.hasText(name) || name.contains("..")) throw new IllegalArgumentException("Invalid file name");
        AttachmentValidation.validate(file, "PHOTO".equals(type) ? List.of("jpg", "jpeg", "png") : List.of("pdf", "doc", "docx", "xls", "xlsx"), MAX_FILE_SIZE, name + " exceeds the 10 MB file limit", "PHOTO".equals(type) ? "Photo must be JPG, JPEG or PNG" : "Document must be PDF, DOC, DOCX, XLS or XLSX");
        String ext = extension(name), uuid = UUID.randomUUID().toString().replace("-", "");
        Path dir = Paths.get(storageDirectory).toAbsolutePath().normalize().resolve(type.toLowerCase()), path = dir.resolve(uuid + "." + ext);
        try {
            Files.createDirectories(dir);
            try (var input = file.getInputStream()) {
                Files.copy(input, path, StandardCopyOption.REPLACE_EXISTING);
            }
            FileMapping mapping = new FileMapping();
            mapping.setUuid(uuid);
            mapping.setFileName(name);
            mapping.setFilePath(path.toAbsolutePath().toString());
            mapping.setFileType(type);
            repository.saveAndFlush(mapping);
            return new ContactFileResponseDto(uuid, name, type);
        } catch (Exception e) {
            try {
                Files.deleteIfExists(path);
            } catch (Exception ignored) {
            }
            throw new IllegalStateException("Unable to store file", e);
        }
    }

    private FilePaths paths(FileMapping mapping) {
        String ext = extension(mapping.getFileName()), folder = Objects.toString(mapping.getFileType(), "file").toLowerCase(), saved = mapping.getUuid() + (StringUtils.hasText(ext) ? "." + ext : "");
        Path root = Paths.get(storageDirectory).toAbsolutePath().normalize(), cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize(), stable = root.resolve(folder).resolve(saved);
        Set<Path> paths = new LinkedHashSet<>(List.of(stable, cwd.resolve("uploads").resolve(folder).resolve(saved)));
        if (StringUtils.hasText(mapping.getFilePath())) try {
            paths.add(Paths.get(mapping.getFilePath()).toAbsolutePath().normalize());
        } catch (Exception ignored) {
        }
        Path parent = cwd.getParent();
        if (parent != null) {
            paths.add(parent.resolve("uploads").resolve(folder).resolve(saved));
            paths.add(parent.resolve("contact-management").resolve("uploads").resolve(folder).resolve(saved));
        }
        return new FilePaths(stable, paths);
    }

    private boolean valid(String uuid) {
        return StringUtils.hasText(uuid) && uuid.matches("^[a-fA-F0-9]{32}$");
    }

    private String extension(String name) {
        return Objects.toString(StringUtils.getFilenameExtension(name), "").toLowerCase();
    }

    private MediaType mediaType(String ext) {
        return switch (ext) {
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG;
            case "png" -> MediaType.IMAGE_PNG;
            case "pdf" -> MediaType.APPLICATION_PDF;
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }

    private record FilePaths(Path stable, Set<Path> candidates) {
    }
}
