package com.contactmanagement.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AttachmentValidation {
    public static void validate(MultipartFile file, Collection<String> allowed, long max, String sizeMessage, String formatMessage) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Invalid or empty file");
        if (file.getSize() > max) throw new IllegalArgumentException(sizeMessage);
        String ext = Objects.toString(StringUtils.getFilenameExtension(Objects.toString(file.getOriginalFilename(), "")), "").toLowerCase(Locale.ROOT);
        if (!allowed.contains(ext)) throw new IllegalArgumentException(formatMessage);
    }
}
