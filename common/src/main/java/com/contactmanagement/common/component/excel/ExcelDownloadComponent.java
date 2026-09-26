package com.contactmanagement.common.component.excel;

import org.springframework.http.*;
import org.springframework.stereotype.Component;

@Component
public class ExcelDownloadComponent {
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    public ResponseEntity<byte[]> download(String fileName, byte[] data) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"").contentType(XLSX).body(data);
    }
}
