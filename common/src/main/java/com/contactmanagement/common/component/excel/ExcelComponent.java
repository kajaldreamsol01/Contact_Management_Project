package com.contactmanagement.common.component.excel;

import com.contactmanagement.common.dto.ContactDataDto;
import com.contactmanagement.common.dto.ContactExistingResponseDto;
import com.contactmanagement.common.dto.ContactRequestDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ExcelComponent {
    private static ContactRequestDto fromRow(Row row, Map<String, Integer> columns) {
        ContactRequestDto request = new ContactRequestDto();
        request.setContactCode(cellValue(row, columns, "contactCode"));
        request.setContactType(cellValue(row, columns, "contactType"));
        request.setName(cellValue(row, columns, "name"));
        request.setCommunicationName(cellValue(row, columns, "communicationName"));
        request.setDepartment(cellValue(row, columns, "department"));
        request.setDesignation(cellValue(row, columns, "designation"));
        request.setCompanyName(cellValue(row, columns, "companyName"));
        request.setMobile(cellValue(row, columns, "mobile"));
        request.setAlternateMobile(cellValue(row, columns, "alternateMobile"));
        request.setOfficeNumber(cellValue(row, columns, "officeNumber"));
        request.setEmail(cellValue(row, columns, "email"));
        request.setAlternateEmail(cellValue(row, columns, "alternateEmail"));
        request.setEmployeeId(cellValue(row, columns, "employeeId"));
        request.setGender(cellValue(row, columns, "gender"));
        request.setMaritalStatus(cellValue(row, columns, "maritalStatus"));
        request.setDateOfBirth(dateValue(row, columns, "dateOfBirth"));
        request.setAnniversaryDate(dateValue(row, columns, "anniversaryDate"));
        request.setBloodGroup(cellValue(row, columns, "bloodGroup"));
        request.setCountry(cellValue(row, columns, "country"));
        request.setState(cellValue(row, columns, "state"));
        request.setCity(cellValue(row, columns, "city"));
        request.setAddress(cellValue(row, columns, "address"));
        request.setPinCode(cellValue(row, columns, "pinCode"));
        request.setSkills(splitValues(cellValue(row, columns, "skills")));
        request.setLanguages(splitValues(cellValue(row, columns, "languages")));
        request.setEmergencyContactName(cellValue(row, columns, "emergencyContactName"));
        request.setEmergencyContactNumber(cellValue(row, columns, "emergencyContactNumber"));
        request.setRemarks(cellValue(row, columns, "remarks"));
        request.setStatus(parseStatus(cellValue(row, columns, "status")));
        return request;
    }

    private static boolean parseStatus(String value) {
        String status = Objects.toString(value, "").trim().toLowerCase(Locale.ROOT);
        if (status.isBlank() || List.of("active", "false", "0", "no").contains(status)) {
            return false;
        }
        if (List.of("inactive", "true", "1", "yes").contains(status)) {
            return true;
        }
        throw new IllegalArgumentException("Status must be Active or Inactive");
    }

    public static List<ExcelRow> read(MultipartFile file, Validator validator, List<ContactExistingResponseDto> existing) throws Exception {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new IllegalArgumentException("Please select Excel file");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (!StringUtils.hasText(extension) || !List.of("xls", "xlsx").contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Only .xlsx and .xls files are allowed");
        }
        List<ExcelRow> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel sheet is missing");
            }
            Sheet sheet = workbook.getSheetAt(0);
            Row first = sheet.getRow(0);
            Row second = sheet.getRow(1);
            if (Objects.isNull(first)) {
                throw new IllegalArgumentException("Excel header row is missing");
            }
            DataFormatter formatter = new DataFormatter();
            boolean typeRow = false;
            for (Cell cell : first) {
                String current = formatter.formatCellValue(cell).trim();
                if ("Mandatory".equalsIgnoreCase(current) || "Optional".equalsIgnoreCase(current)) {
                    typeRow = true;
                    break;
                }
            }
            Row header = typeRow && Objects.nonNull(second) ? second : first;
            Map<String, Integer> columns = columnIndexes(header);
            List<String> missing = ExcelHeader.UPLOAD_COLUMNS.entrySet().stream().filter(entry -> !columns.containsKey(entry.getKey())).map(Map.Entry::getValue).toList();
            if (ExcelHeader.UPLOAD_COLUMNS.keySet().stream().noneMatch(columns::containsKey)) {
                throw new IllegalArgumentException("Invalid Excel file. Contact columns not found");
            }
            String columnError = missing.isEmpty() ? null : "Missing column(s): " + String.join(", ", missing);
            for (int index = header.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (Objects.isNull(row) || isEmptyRow(row)) {
                    continue;
                }
                try {
                    ContactRequestDto request = fromRow(row, columns);
                    String error = validator.validate(request).stream().map(ConstraintViolation::getMessage).distinct().collect(Collectors.joining(", "));
                    if (Objects.nonNull(columnError)) {
                        error = error.isBlank() ? columnError : columnError + ", " + error;
                    }
                    rows.add(new ExcelRow(index + 1, request, error.isBlank() ? null : error, null, null, List.of()));
                } catch (Exception exception) {
                    rows.add(new ExcelRow(index + 1, new ContactRequestDto(), Objects.isNull(exception.getMessage()) ? "Invalid row data" : exception.getMessage(), null, null, List.of()));
                }
            }
        }
        existing = Objects.requireNonNullElse(existing, List.of());
        Map<String, ContactExistingResponseDto> existingCodes = existing.stream()
                .filter(contact -> StringUtils.hasText(contact.getContactCode()))
                .collect(Collectors.toMap(contact -> contact.getContactCode().trim().toLowerCase(Locale.ROOT), contact -> contact, (a, b) -> a));
        Map<String, ContactExistingResponseDto> existingMobiles = existing.stream().filter(contact -> StringUtils.hasText(contact.getMobile())).collect(Collectors.toMap(ContactExistingResponseDto::getMobile, contact -> contact, (a, b) -> a));
        Map<String, ContactExistingResponseDto> existingEmails = existing.stream().filter(contact -> StringUtils.hasText(contact.getEmail())).collect(Collectors.toMap(contact -> contact.getEmail().toLowerCase(Locale.ROOT), contact -> contact, (a, b) -> a));
        Set<String> seenMobiles = new HashSet<>();
        Set<String> seenEmails = new HashSet<>();
        List<ExcelRow> checked = new ArrayList<>(rows.size());
        for (ExcelRow row : rows) {
            String error = row.error();
            String previousStatus = null;
            String updateType = "New";
            List<String> changedFields = List.of();
            ContactRequestDto request = row.request();
            if (Objects.isNull(error)) {
                String mobile = request.getMobile();
                String email = Objects.toString(request.getEmail(), "").toLowerCase(Locale.ROOT);
                if (!seenMobiles.add(mobile)) {
                    error = "Duplicate mobile in Excel";
                } else if (!seenEmails.add(email)) {
                    error = "Duplicate email in Excel";
                } else {
                    String contactCode = Objects.toString(request.getContactCode(), "").trim().toLowerCase(Locale.ROOT);
                    ContactExistingResponseDto contact = StringUtils.hasText(contactCode) ? existingCodes.get(contactCode) : null;
                    if (Objects.isNull(contact)) {
                        contact = existingMobiles.get(mobile);
                    }
                    if (Objects.isNull(contact)) {
                        contact = existingEmails.get(email);
                    }
                    if (Objects.nonNull(contact)) {
                        previousStatus = contact.isStatus() ? "Inactive" : "Active";
                        changedFields = changedFields(contact, request);
                        if (changedFields.isEmpty()) {
                            error = "Already exists - no changes";
                            updateType = "No Change";
                        } else {
                            request.setId(contact.getId());
                            updateType = "Update";
                        }
                    }
                }
            }
            checked.add(new ExcelRow(row.number(), request, error, previousStatus, updateType, changedFields));
        }
        return checked;
    }

    private static List<String> changedFields(ContactExistingResponseDto contact, ContactRequestDto request) {
        List<String> changed = new ArrayList<>();
        addChanged(changed, "Contact Type", contact.getContactType(), request.getContactType());
        addChanged(changed, "Name", contact.getName(), request.getName());
        addChanged(changed, "Communication Name", contact.getCommunicationName(), request.getCommunicationName());
        addChanged(changed, "Department", contact.getDepartment(), request.getDepartment());
        addChanged(changed, "Designation", contact.getDesignation(), request.getDesignation());
        addChanged(changed, "Company Name", contact.getCompanyName(), request.getCompanyName());
        addChanged(changed, "Mobile", contact.getMobile(), request.getMobile());
        addChanged(changed, "Alternate Mobile", contact.getAlternateMobile(), request.getAlternateMobile());
        addChanged(changed, "Office Number", contact.getOfficeNumber(), request.getOfficeNumber());
        addChanged(changed, "Email", contact.getEmail(), request.getEmail());
        addChanged(changed, "Alternate Email", contact.getAlternateEmail(), request.getAlternateEmail());
        addChanged(changed, "Employee ID", contact.getEmployeeId(), request.getEmployeeId());
        addChanged(changed, "Gender", contact.getGender(), request.getGender());
        addChanged(changed, "Marital Status", contact.getMaritalStatus(), request.getMaritalStatus());
        if (!Objects.equals(contact.getDateOfBirth(), request.getDateOfBirth())) {
            changed.add("Date Of Birth");
        }
        if (!Objects.equals(contact.getAnniversaryDate(), request.getAnniversaryDate())) {
            changed.add("Anniversary Date");
        }
        addChanged(changed, "Blood Group", contact.getBloodGroup(), request.getBloodGroup());
        addChanged(changed, "Country", contact.getCountry(), request.getCountry());
        addChanged(changed, "State", contact.getState(), request.getState());
        addChanged(changed, "City", contact.getCity(), request.getCity());
        addChanged(changed, "Address", contact.getAddress(), request.getAddress());
        addChanged(changed, "Pin Code", contact.getPinCode(), request.getPinCode());
        if (listsDiffer(contact.getSkills(), request.getSkills())) {
            changed.add("Skills");
        }
        if (listsDiffer(contact.getLanguages(), request.getLanguages())) {
            changed.add("Languages");
        }
        addChanged(changed, "Emergency Contact Name", contact.getEmergencyContactName(), request.getEmergencyContactName());
        addChanged(changed, "Emergency Contact Number", contact.getEmergencyContactNumber(), request.getEmergencyContactNumber());
        addChanged(changed, "Remarks", contact.getRemarks(), request.getRemarks());
        if (contact.isStatus() != request.isStatus()) {
            changed.add("Status (" + (contact.isStatus() ? "Inactive" : "Active") + " → " + (request.isStatus() ? "Inactive" : "Active") + ")");
        }
        return changed;
    }

    private static void addChanged(List<String> changed, String field, String oldValue, String newValue) {
        if (!same(oldValue, newValue)) {
            changed.add(field);
        }
    }

    private static boolean same(String first, String second) {
        return Objects.toString(first, "").trim().equalsIgnoreCase(Objects.toString(second, "").trim());
    }

    private static boolean listsDiffer(List<String> first, List<String> second) {
        List<String> left = (first == null ? List.<String>of() : first).stream().map(value -> value.trim().toLowerCase(Locale.ROOT)).sorted().toList();
        List<String> right = (second == null ? List.<String>of() : second).stream().map(value -> value.trim().toLowerCase(Locale.ROOT)).sorted().toList();
        return !left.equals(right);
    }

    public static List<Map<String, Object>> preview(List<ExcelRow> rows) {
        return rows.stream().map(row -> {
            ContactRequestDto request = row.request();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rowNumber", row.number());
            item.put("contactType", request.getContactType());
            item.put("name", request.getName());
            item.put("mobile", request.getMobile());
            item.put("email", request.getEmail());
            item.put("department", request.getDepartment());
            item.put("designation", request.getDesignation());
            item.put("companyName", request.getCompanyName());
            item.put("city", request.getCity());
            item.put("previousStatus", row.previousStatus());
            item.put("status", request.isStatus() ? "Inactive" : "Active");
            item.put("updateType", row.updateType());
            item.put("changedFields", String.join(", ", row.changedFields()));
            item.put("action", Objects.nonNull(request.getId()) ? "Update" : "New");
            item.put("error", row.error());
            return item;
        }).toList();
    }

    public static Map<String, Object> validate(MultipartFile file, Validator validator, List<ContactExistingResponseDto> existing) throws Exception {
        List<ExcelRow> rows = read(file, validator, existing);
        List<Map<String, Object>> records = preview(rows);
        List<Map<String, Object>> savable = records.stream().filter(row -> Objects.isNull(row.get("error"))).toList();
        List<Map<String, Object>> newRecords = savable.stream().filter(row -> "New".equals(row.get("action"))).toList();
        List<Map<String, Object>> updateRecords = savable.stream().filter(row -> "Update".equals(row.get("action"))).toList();
        List<Map<String, Object>> duplicates = records.stream().filter(row -> isDuplicate(row.get("error"))).toList();
        List<Map<String, Object>> invalid = records.stream().filter(row -> Objects.nonNull(row.get("error")) && !isDuplicate(row.get("error"))).toList();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("valid", invalid.isEmpty());
        data.put("canSave", !savable.isEmpty());
        data.put("records", records);
        data.put("newRecords", newRecords);
        data.put("updateRecords", updateRecords);
        data.put("duplicates", duplicates);
        data.put("invalid", invalid);
        data.put("newCount", newRecords.size());
        data.put("updateCount", updateRecords.size());
        data.put("duplicateCount", duplicates.size());
        data.put("invalidCount", invalid.size());
        return data;
    }

    public static ImportBatch prepareImport(MultipartFile file, Validator validator, List<ContactExistingResponseDto> existing) throws Exception {
        List<ExcelRow> rows = read(file, validator, existing);
        List<ExcelRow> savableRows = rows.stream().filter(row -> !StringUtils.hasText(row.error())).toList();
        List<ContactRequestDto> requests = savableRows.stream().map(ExcelRow::request).toList();
        List<Map<String, Object>> duplicates = rows.stream().filter(row -> isDuplicate(row.error())).map(ExcelComponent::item).toList();
        List<Map<String, Object>> invalid = rows.stream().filter(row -> StringUtils.hasText(row.error()) && !isDuplicate(row.error())).map(ExcelComponent::item).toList();
        return new ImportBatch(rows.size(), rows, requests, savableRows, duplicates, invalid);
    }

    private static Map<String, Object> item(ExcelRow row) {
        ContactRequestDto request = row.request();
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("index", row.number());
        item.put("rowNumber", row.number());
        item.put("contactType", request.getContactType());
        item.put("name", request.getName());
        item.put("mobile", request.getMobile());
        item.put("email", request.getEmail());
        item.put("department", request.getDepartment());
        item.put("designation", request.getDesignation());
        item.put("companyName", request.getCompanyName());
        item.put("city", request.getCity());
        item.put("status", request.isStatus() ? "Inactive" : "Active");
        item.put("changedFields", String.join(", ", row.changedFields()));
        item.put("message", row.error());
        return item;
    }

    public static boolean isDuplicate(Object error) {
        String value = Objects.toString(error, "").toLowerCase(Locale.ROOT);
        return value.contains("duplicate") || value.contains("already exists");
    }

    public static Map<String, Object> result(int total, List<Map<String, Object>> newSaved, List<Map<String, Object>> updated, List<Map<String, Object>> duplicates, List<Map<String, Object>> invalid) {
        List<Map<String, Object>> saved = new ArrayList<>(newSaved);
        saved.addAll(updated);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalCount", total);
        data.put("successCount", saved.size());
        data.put("newSavedCount", newSaved.size());
        data.put("updatedCount", updated.size());
        data.put("duplicateCount", duplicates.size());
        data.put("invalidCount", invalid.size());
        data.put("saved", saved);
        data.put("newSaved", newSaved);
        data.put("updated", updated);
        data.put("duplicates", duplicates);
        data.put("invalid", invalid);
        return data;
    }

    public static Map<String, Object> item(int index, ContactRequestDto request, String value) {
        ContactRequestDto dataRequest = Objects.requireNonNullElseGet(request, ContactRequestDto::new);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("index", index);
        data.put("rowNumber", index);
        data.put("contactType", dataRequest.getContactType());
        data.put("name", dataRequest.getName());
        data.put("mobile", dataRequest.getMobile());
        data.put("email", dataRequest.getEmail());
        data.put("department", dataRequest.getDepartment());
        data.put("designation", dataRequest.getDesignation());
        data.put("companyName", dataRequest.getCompanyName());
        data.put("city", dataRequest.getCity());
        data.put(value.startsWith("CNT") ? "contactCode" : "message", value);
        return data;
    }

    public record ImportBatch(int totalCount, List<ExcelRow> allRows, List<ContactRequestDto> requests, List<ExcelRow> savableRows,
                              List<Map<String, Object>> duplicates, List<Map<String, Object>> invalid) {
    }

    public static byte[] exportImportResult(
            ImportBatch batch,
            List<Map<String, Object>> saved,
            List<Map<String, Object>> duplicates,
            List<Map<String, Object>> invalid
    ) throws Exception {
        String[] headers = Arrays.copyOf(ExcelHeader.HEADERS, ExcelHeader.HEADERS.length + 4);
        headers[ExcelHeader.HEADERS.length] = "Upload Status";
        headers[ExcelHeader.HEADERS.length + 1] = "Action";
        headers[ExcelHeader.HEADERS.length + 2] = "Remark";
        headers[ExcelHeader.HEADERS.length + 3] = "Changed Fields";

        Map<Integer, Map<String, Object>> resultByRow = new LinkedHashMap<>();
        for (Map<String, Object> row : saved == null ? Collections.<Map<String, Object>>emptyList() : saved) {
            Object indexValue = row.get("index");
            if (!(indexValue instanceof Number number)) continue;
            int requestIndex = number.intValue() - 1;
            if (requestIndex < 0 || requestIndex >= batch.savableRows().size()) continue;
            ExcelRow source = batch.savableRows().get(requestIndex);
            Map<String, Object> copy = new LinkedHashMap<>(row);
            copy.put("uploadStatus", "SUCCESS");
            copy.put("action", Objects.nonNull(source.request().getId()) ? "UPDATED" : "NEW SAVED");
            copy.put("remark", Objects.nonNull(source.request().getId()) ? "Contact updated successfully" : "New contact saved successfully");
            copy.put("changedFields", source.changedFields().isEmpty() ? "" : String.join(", ", source.changedFields()));
            resultByRow.put(source.number(), copy);
        }
        for (Map<String, Object> row : duplicates == null ? Collections.<Map<String, Object>>emptyList() : duplicates) {
            int rowNumber = number(row.get("rowNumber"));
            Map<String, Object> copy = new LinkedHashMap<>(row);
            copy.put("uploadStatus", "FAILED");
            copy.put("action", "DUPLICATE");
            copy.put("remark", Objects.toString(row.get("message"), "Duplicate data"));
            resultByRow.put(rowNumber, copy);
        }
        for (Map<String, Object> row : invalid == null ? Collections.<Map<String, Object>>emptyList() : invalid) {
            int rowNumber = number(row.get("rowNumber"));
            Map<String, Object> copy = new LinkedHashMap<>(row);
            copy.put("uploadStatus", "FAILED");
            copy.put("action", "INVALID");
            copy.put("remark", Objects.toString(row.get("message"), "Invalid data"));
            resultByRow.put(rowNumber, copy);
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Import Result");
            writeContactHeaders(workbook, sheet, headers);
            int rowIndex = 2;
            for (ExcelRow source : batch.allRows()) {
                ContactRequestDto request = source.request();
                Map<String, Object> result = resultByRow.getOrDefault(source.number(), Map.of());
                Object[] values = {
                        Objects.toString(result.get("contactCode"), request.getContactCode()),
                        request.getContactType(), request.getName(), request.getCommunicationName(), request.getDepartment(),
                        request.getDesignation(), request.getCompanyName(), request.getMobile(), request.getAlternateMobile(),
                        request.getOfficeNumber(), request.getEmail(), request.getAlternateEmail(), request.getEmployeeId(),
                        request.getGender(), request.getMaritalStatus(), request.getDateOfBirth(), request.getAnniversaryDate(),
                        request.getBloodGroup(), request.getCountry(), request.getState(), request.getCity(), request.getAddress(),
                        request.getPinCode(), request.getSkills(), request.getLanguages(), request.getEmergencyContactName(),
                        request.getEmergencyContactNumber(), request.getRemarks(), request.isStatus() ? "Inactive" : "Active",
                        Objects.toString(result.get("uploadStatus"), "FAILED"),
                        Objects.toString(result.get("action"), StringUtils.hasText(source.error()) ? "INVALID" : "NOT SAVED"),
                        Objects.toString(result.get("remark"), Objects.toString(source.error(), "Not saved")),
                        Objects.toString(result.get("changedFields"), source.changedFields().isEmpty() ? "" : String.join(", ", source.changedFields()))
                };
                Row excelRow = sheet.createRow(rowIndex++);
                for (int column = 0; column < values.length; column++) {
                    excelRow.createCell(column).setCellValue(cellText(values[column]));
                }
            }
            sheet.createFreezePane(0, 2);
            sheet.setAutoFilter(new CellRangeAddress(1, Math.max(1, rowIndex - 1), 0, headers.length - 1));
            for (int index = 0; index < headers.length; index++) {
                sheet.autoSizeColumn(index);
                sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 800, 18000));
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static int number(Object value) {
        if (value instanceof Number n) return n.intValue();
        try { return Integer.parseInt(Objects.toString(value, "0")); }
        catch (Exception ignored) { return 0; }
    }

    public static byte[] exportSaved(List<ExcelRow> rows, List<Map<String, Object>> saved) throws Exception {
        String[] resultHeaders = Arrays.copyOf(ExcelHeader.HEADERS, ExcelHeader.HEADERS.length + 2);
        resultHeaders[ExcelHeader.HEADERS.length] = "Save Type";
        resultHeaders[ExcelHeader.HEADERS.length + 1] = "Changed Fields";
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Import Result");
            writeContactHeaders(workbook, sheet, resultHeaders);
            int rowIndex = 2;
            for (Map<String, Object> savedRow : saved) {
                Object indexValue = savedRow.get("index");
                if (!(indexValue instanceof Number number)) {
                    continue;
                }
                int index = number.intValue() - 1;
                if (index < 0 || index >= rows.size()) {
                    continue;
                }
                ExcelRow source = rows.get(index);
                ContactRequestDto request = source.request();
                Object[] values = {Objects.toString(savedRow.get("contactCode"), ""), request.getContactType(), request.getName(), request.getCommunicationName(), request.getDepartment(), request.getDesignation(), request.getCompanyName(), request.getMobile(), request.getAlternateMobile(), request.getOfficeNumber(), request.getEmail(), request.getAlternateEmail(), request.getEmployeeId(), request.getGender(), request.getMaritalStatus(), request.getDateOfBirth(), request.getAnniversaryDate(), request.getBloodGroup(), request.getCountry(), request.getState(), request.getCity(), request.getAddress(), request.getPinCode(), request.getSkills(), request.getLanguages(), request.getEmergencyContactName(), request.getEmergencyContactNumber(), request.getRemarks(), request.isStatus() ? "Inactive" : "Active", Objects.nonNull(request.getId()) ? "Updated" : "New Saved", source.changedFields().isEmpty() ? "-" : String.join(", ", source.changedFields())};
                Row excelRow = sheet.createRow(rowIndex++);
                for (int column = 0; column < values.length; column++) {
                    excelRow.createCell(column).setCellValue(cellText(values[column]));
                }
            }
            sheet.createFreezePane(0, 2);
            sheet.setAutoFilter(new CellRangeAddress(1, Math.max(1, rowIndex - 1), 0, resultHeaders.length - 1));
            for (int index = 0; index < resultHeaders.length; index++) {
                sheet.autoSizeColumn(index);
                sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 800, 16000));
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    public static byte[] export(List<ContactDataDto> contacts) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Contacts");
            writeContactHeaders(workbook, sheet, ExcelHeader.HEADERS);
            int rowIndex = 2;
            for (ContactDataDto contact : contacts) {
                Object[] values = {contact.getContactCode(), contact.getContactType(), contact.getName(), contact.getCommunicationName(), contact.getDepartment(), contact.getDesignation(), contact.getCompanyName(), contact.getMobile(), contact.getAlternateMobile(), contact.getOfficeNumber(), contact.getEmail(), contact.getAlternateEmail(), contact.getEmployeeId(), contact.getGender(), contact.getMaritalStatus(), contact.getDateOfBirth(), contact.getAnniversaryDate(), contact.getBloodGroup(), contact.getCountry(), contact.getState(), contact.getCity(), contact.getAddress(), contact.getPinCode(), contact.getSkills(), contact.getLanguages(), contact.getEmergencyContactName(), contact.getEmergencyContactNumber(), contact.getRemarks(), contact.isStatus() ? "Inactive" : "Active"};
                Row row = sheet.createRow(rowIndex++);
                for (int index = 0; index < values.length; index++) {
                    row.createCell(index).setCellValue(cellText(values[index]));
                }
            }
            sheet.createFreezePane(0, 2);
            sheet.setAutoFilter(new CellRangeAddress(1, Math.max(1, rowIndex - 1), 0, ExcelHeader.HEADERS.length - 1));
            for (int index = 0; index < ExcelHeader.HEADERS.length; index++) {
                sheet.autoSizeColumn(index);
                sheet.setColumnWidth(index, Math.min(sheet.getColumnWidth(index) + 800, 12000));
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private static Map<String, Integer> columnIndexes(Row row) {
        Map<String, Integer> columns = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        row.forEach(cell -> {
            String header = formatter.formatCellValue(cell).trim().toLowerCase(Locale.ROOT);
            String field = ExcelHeader.HEADER_MAP.get(header);
            if (StringUtils.hasText(field)) {
                columns.putIfAbsent(field, cell.getColumnIndex());
            }
        });
        return columns;
    }

    private static boolean isEmptyRow(Row row) {
        DataFormatter formatter = new DataFormatter();
        for (Cell cell : row) {
            if (StringUtils.hasText(formatter.formatCellValue(cell))) {
                return false;
            }
        }
        return true;
    }

    private static String cellValue(Row row, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        if (Objects.isNull(index)) {
            return "";
        }
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (Objects.isNull(cell)) {
            return "";
        }
        String value = new DataFormatter().formatCellValue(cell).trim();
        return isPlaceholder(value) ? "" : value;
    }

    private static LocalDate dateValue(Row row, Map<String, Integer> columns, String name) {
        Integer index = columns.get(name);
        if (Objects.isNull(index)) {
            return null;
        }
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (Objects.isNull(cell)) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        String value = new DataFormatter().formatCellValue(cell).trim();
        if (value.isBlank() || isPlaceholder(value)) {
            return null;
        }
        for (DateTimeFormatter format : List.of(DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("dd-MM-yyyy"), DateTimeFormatter.ofPattern("dd/MM/yyyy"))) {
            try {
                return LocalDate.parse(value, format);
            } catch (Exception ignored) {
            }
        }
        throw new IllegalArgumentException("Invalid " + name + " date");
    }

    private static List<String> splitValues(String value) {
        return StringUtils.hasText(value) ? Arrays.stream(value.split("[,;]")).map(String::trim).filter(StringUtils::hasText).distinct().toList() : List.of();
    }

    private static String cellText(Object value) {
        if (Objects.isNull(value)) return "N/A";
        if (value instanceof LocalDate date)
            return date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        if (value instanceof Collection<?> collection) {
            String text = collection.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining(", "));
            return StringUtils.hasText(text) ? text : "N/A";
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : "N/A";
    }

    private static boolean isPlaceholder(String value) {
        String normalized = Objects.toString(value, "").trim().toLowerCase(Locale.ROOT);
        return normalized.equals("n/a") || normalized.equals("na");
    }

    private static void writeContactHeaders(Workbook workbook, Sheet sheet, String[] headers) {
        Row typeRow = sheet.createRow(0);
        Row headerRow = sheet.createRow(1);
        CellStyle requiredLabel = cellStyle(workbook);
        CellStyle optionalLabel = optionalLabelStyle(workbook);
        CellStyle headerStyle = themeHeaderStyle(workbook);
        for (int index = 0; index < headers.length; index++) {
            boolean required = index < ExcelHeader.HEADERS.length && ExcelHeader.REQUIRED.contains(index);
            Cell label = typeRow.createCell(index);
            label.setCellValue(required ? "Mandatory" : "Optional");
            label.setCellStyle(required ? requiredLabel : optionalLabel);
            Cell header = headerRow.createCell(index);
            header.setCellValue(headers[index]);
            header.setCellStyle(headerStyle);
        }
    }

    private static CellStyle themeHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{15, 118, 110}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle optionalLabelStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle cellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFillForegroundColor(IndexedColors.RED.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    public record ExcelRow(int number, ContactRequestDto request, String error, String previousStatus,
                           String updateType, List<String> changedFields) {
    }
}
