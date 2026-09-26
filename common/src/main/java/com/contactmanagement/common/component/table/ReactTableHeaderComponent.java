package com.contactmanagement.common.component.table;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class ReactTableHeaderComponent {

    public List<TableColumnConfig> get(String table) {
        return switch (Objects.toString(table, "").trim().toUpperCase(Locale.ROOT)) {
            case "CONTACT" -> contact();
            case "CONTACT_HISTORY" -> history();
            case "CONTACT_EXCEL_PREVIEW" -> excelPreview();
            case "DASHBOARD_CONTACT_TYPE" -> summary("contactType", "Contact Type");
            case "DASHBOARD_DEPARTMENT" -> summary("department", "Department");
            case "DASHBOARD_CITY" -> summary("city", "City");
            default -> List.of();
        };
    }

    private List<TableColumnConfig> contact() {
        return List.of(
                column("action", "Action", 1, 100, true, false, "ACTION"),
                column("contactCode", "Contact Code", 2, 145, true, true, "CONTACT_CODE"),
                column("name", "Name", 3, 160, true, true, "TEXT"),
                column("contactType", "Contact Type", 4, 145, true, true, "TEXT"),
                column("mobile", "Mobile Number", 5, 150, true, true, "TEXT"),
                column("email", "Email ID", 6, 220, true, true, "TEXT"),
                column("department", "Department", 7, 145, true, true, "TEXT"),
                column("designation", "Designation", 8, 160, true, true, "TEXT"),
                column("companyName", "Company Name", 9, 180, true, true, "TEXT"),
                column("city", "City", 10, 130, true, true, "TEXT"),
                column("photo", "Photo", 11, 90, true, false, "PHOTO"),
                column("documents", "Documents", 12, 100, true, false, "DOCUMENTS"),
                column("status", "Status", 13, 120, true, true, "STATUS"),
                column("createdBy", "Created By", 14, 150, true, false, "CREATED_BY"),
                column("createdAt", "Created At", 15, 170, true, true, "DATETIME"),
                column("updatedBy", "Updated By", 16, 150, true, false, "UPDATED_BY"),
                column("updatedAt", "Updated At", 17, 170, true, true, "DATETIME")
        );
    }

    private List<TableColumnConfig> history() {
        return List.of(
                column("action", "Action", 1, 85, true, false, "HISTORY_ACTION"),
                column("actionBy", "Action By", 2, 115, true, false, "TEXT"),
                column("changedAt", "Date / Time", 3, 135, true, false, "DATETIME"),
                column("contactType", "Contact Type", 4, 95, true, false, "TEXT"),
                column("name", "Name", 5, 110, true, false, "TEXT"),
                column("communicationName", "Communication Name", 6, 105, true, false, "TEXT"),
                column("department", "Department", 7, 100, true, false, "TEXT"),
                column("designation", "Designation", 8, 100, true, false, "TEXT"),
                column("companyName", "Company Name", 9, 100, true, false, "TEXT"),
                column("mobile", "Mobile", 10, 100, true, false, "TEXT"),
                column("alternateMobile", "Alternate Mobile", 11, 100, true, false, "TEXT"),
                column("officeNumber", "Office Number", 12, 105, true, false, "TEXT"),
                column("email", "Email", 13, 115, true, false, "TEXT"),
                column("alternateEmail", "Alternate Email", 14, 115, true, false, "TEXT"),
                column("employeeId", "Employee ID", 15, 95, true, false, "TEXT"),
                column("gender", "Gender", 16, 80, true, false, "TEXT"),
                column("maritalStatus", "Marital Status", 17, 100, true, false, "TEXT"),
                column("dateOfBirth", "Date Of Birth", 18, 100, true, false, "TEXT"),
                column("anniversaryDate", "Anniversary Date", 19, 110, true, false, "TEXT"),
                column("bloodGroup", "Blood Group", 20, 90, true, false, "TEXT"),
                column("country", "Country", 21, 90, true, false, "TEXT"),
                column("state", "State", 22, 90, true, false, "TEXT"),
                column("city", "City", 23, 90, true, false, "TEXT"),
                column("address", "Address", 24, 120, true, false, "TEXT"),
                column("pinCode", "Pin Code", 25, 85, true, false, "TEXT"),
                column("skills", "Skills", 26, 95, true, false, "TEXT"),
                column("languages", "Languages", 27, 95, true, false, "TEXT"),
                column("emergencyContactName", "Emergency Contact Name", 28, 115, true, false, "TEXT"),
                column("emergencyContactNumber", "Emergency Contact Number", 29, 120, true, false, "TEXT"),
                column("photoUuid", "Photo", 30, 70, true, false, "PHOTO"),
                column("documentUuids", "Documents", 31, 80, true, false, "DOCUMENTS"),
                column("remarks", "Remarks", 32, 115, true, false, "TEXT"),
                column("status", "Status", 33, 80, true, false, "STATUS")
        );
    }

    private List<TableColumnConfig> excelPreview() {
        return List.of(
                column("contactType", "Contact Type", 1, 145, true, false, "TEXT"),
                column("name", "Name", 2, 160, true, false, "TEXT"),
                column("mobile", "Mobile", 3, 135, true, false, "TEXT"),
                column("email", "Email", 4, 210, true, false, "TEXT"),
                column("department", "Department", 5, 145, true, false, "TEXT"),
                column("designation", "Designation", 6, 155, true, false, "TEXT"),
                column("companyName", "Company Name", 7, 175, true, false, "TEXT"),
                column("city", "City", 8, 125, true, false, "TEXT"),
                column("previousStatus", "Previous Status", 9, 135, true, false, "PREVIOUS_STATUS"),
                column("status", "New Status", 10, 120, true, false, "STATUS_TEXT"),
                column("action", "Action", 11, 110, true, false, "EXCEL_ACTION"),
                column("updateType", "Update Type", 12, 135, true, false, "TEXT"),
                column("changedFields", "Changed Fields", 13, 220, true, false, "TEXT"),
                column("error", "Result", 14, 220, true, false, "EXCEL_RESULT")
        );
    }

    private List<TableColumnConfig> summary(String key, String header) {
        return List.of(column(key, header, 1, 180, true, true, "TEXT"), column("count", "Count", 2, 100, true, true, "COUNT"), column("share", "Share %", 3, 100, true, true, "PERCENT"));
    }

    private TableColumnConfig column(String key, String header, int order, int size, boolean visible, boolean sortable, String renderer) {
        return new TableColumnConfig(key, header, order, size, visible, sortable, renderer);
    }
}
