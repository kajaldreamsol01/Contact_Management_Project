package com.contactmanagement.common.component.table;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class ReactTableHeaderComponent {
    public Map<String, List<TableColumnConfig>> dashboard() {
        return Map.of("DASHBOARD_DETAIL", dashboardDetail(), "DASHBOARD_CONTACT_TYPE", dashboardContactType(), "DASHBOARD_STATUS", dashboardStatus(), "DASHBOARD_CONTACT_TYPE_STATUS", dashboardContactTypeStatus(), "DASHBOARD_CONTACT_TYPE_TREND", dashboardContactTypeTrend(), "DASHBOARD_DEPARTMENT", summary("department", "Department", "count", "Count", "share", "Share %"), "DASHBOARD_CITY", summary("city", "City", "count", "Count", "share", "Share %"), "CONTACT_HISTORY", history());
    }

    public List<TableColumnConfig> get(String table) {
        return switch (Objects.toString(table, "").trim().toUpperCase(Locale.ROOT)) {
            case "CONTACT" -> contact();
            case "CONTACT_HISTORY" -> history();
            case "CONTACT_EXCEL_PREVIEW" -> excelPreview();
            case "DASHBOARD_DETAIL" -> dashboardDetail();
            case "DASHBOARD_CONTACT_TYPE" -> dashboardContactType();
            case "DASHBOARD_STATUS" -> dashboardStatus();
            case "DASHBOARD_CONTACT_TYPE_STATUS" -> dashboardContactTypeStatus();
            case "DASHBOARD_CONTACT_TYPE_TREND" -> dashboardContactTypeTrend();
            case "DASHBOARD_DEPARTMENT" -> summary("department", "Department", "count", "Count", "share", "Share %");
            case "DASHBOARD_CITY" -> summary("city", "City", "count", "Count", "share", "Share %");
            default -> List.of();
        };
    }

    private List<TableColumnConfig> contact() {
        return List.of(column("action", "Action", 1, 100, true, false, "ACTION"), column("contactCode", "Contact Code", 2, 145, true, true, "CONTACT_CODE"), column("name", "Name", 3, 160, true, true, "TEXT"), column("contactType", "Contact Type", 4, 145, true, true, "TEXT"), column("mobile", "Mobile Number", 5, 150, true, true, "TEXT"), column("email", "Email ID", 6, 220, true, true, "TEXT"), column("department", "Department", 7, 145, true, true, "TEXT"), column("designation", "Designation", 8, 160, true, true, "TEXT"), column("companyName", "Company Name", 9, 180, true, true, "TEXT"), column("city", "City", 10, 130, true, true, "TEXT"), column("photo", "Photo", 11, 90, true, false, "PHOTO"), column("documents", "Documents", 12, 100, true, false, "DOCUMENTS"), column("status", "Status", 13, 120, true, true, "STATUS"), column("createdBy", "Created By", 14, 150, true, false, "CREATED_BY"), column("createdAt", "Created At", 15, 170, true, true, "DATETIME"), column("updatedBy", "Updated By", 16, 150, true, false, "UPDATED_BY"), column("updatedAt", "Updated At", 17, 170, true, true, "DATETIME"));
    }

    private List<TableColumnConfig> history() {
        return List.of(column("action", "Action", 1, 85, true, false, "HISTORY_ACTION"), column("actionBy", "Action By", 2, 115, true, false, "TEXT"), column("changedAt", "Date / Time", 3, 135, true, false, "DATETIME"), column("contactType", "Contact Type", 4, 95, true, false, "TEXT"), column("name", "Name", 5, 110, true, false, "TEXT"), column("communicationName", "Communication Name", 6, 105, true, false, "TEXT"), column("department", "Department", 7, 100, true, false, "TEXT"), column("designation", "Designation", 8, 100, true, false, "TEXT"), column("companyName", "Company Name", 9, 100, true, false, "TEXT"), column("mobile", "Mobile", 10, 100, true, false, "TEXT"), column("alternateMobile", "Alternate Mobile", 11, 100, true, false, "TEXT"), column("officeNumber", "Office Number", 12, 105, true, false, "TEXT"), column("email", "Email", 13, 115, true, false, "TEXT"), column("alternateEmail", "Alternate Email", 14, 115, true, false, "TEXT"), column("employeeId", "Employee ID", 15, 95, true, false, "TEXT"), column("gender", "Gender", 16, 80, true, false, "TEXT"), column("maritalStatus", "Marital Status", 17, 100, true, false, "TEXT"), column("dateOfBirth", "Date Of Birth", 18, 100, true, false, "TEXT"), column("anniversaryDate", "Anniversary Date", 19, 110, true, false, "TEXT"), column("bloodGroup", "Blood Group", 20, 90, true, false, "TEXT"), column("country", "Country", 21, 90, true, false, "TEXT"), column("state", "State", 22, 90, true, false, "TEXT"), column("city", "City", 23, 90, true, false, "TEXT"), column("address", "Address", 24, 120, true, false, "TEXT"), column("pinCode", "Pin Code", 25, 85, true, false, "TEXT"), column("skills", "Skills", 26, 95, true, false, "TEXT"), column("languages", "Languages", 27, 95, true, false, "TEXT"), column("emergencyContactName", "Emergency Contact Name", 28, 115, true, false, "TEXT"), column("emergencyContactNumber", "Emergency Contact Number", 29, 120, true, false, "TEXT"), column("photoUuid", "Photo", 30, 70, true, false, "PHOTO"), column("documentUuids", "Documents", 31, 80, true, false, "DOCUMENTS"), column("remarks", "Remarks", 32, 115, true, false, "TEXT"), column("status", "Status", 33, 80, true, false, "STATUS"));
    }

    private List<TableColumnConfig> dashboardDetail() {
        return List.of(column("contactCode", "Contact Code", 1, 120, true, true, "CONTACT_CODE"), column("name", "Name", 2, 150, true, true, "TEXT"), column("contactType", "Contact Type", 3, 110, true, true, "TEXT"), column("department", "Department", 4, 120, true, true, "TEXT"), column("designation", "Designation", 5, 130, true, true, "TEXT"), column("companyName", "Company", 6, 140, true, true, "TEXT"), column("mobile", "Mobile", 7, 115, true, true, "TEXT"), column("alternateMobile", "Alternate Mobile", 8, 130, true, true, "TEXT"), column("officeNumber", "Office Number", 9, 120, true, true, "TEXT"), column("email", "Email", 10, 190, true, true, "TEXT"), column("alternateEmail", "Alternate Email", 11, 190, true, true, "TEXT"), column("employeeId", "Employee ID", 12, 110, true, true, "TEXT"), column("gender", "Gender", 13, 90, true, true, "TEXT"), column("maritalStatus", "Marital Status", 14, 110, true, true, "TEXT"), column("dateOfBirth", "DOB", 15, 110, true, true, "TEXT"), column("bloodGroup", "Blood Group", 16, 100, true, true, "TEXT"), column("country", "Country", 17, 110, true, true, "TEXT"), column("state", "State", 18, 110, true, true, "TEXT"), column("city", "City", 19, 110, true, true, "TEXT"), column("address", "Address", 20, 220, true, true, "TEXT"), column("pinCode", "Pin Code", 21, 90, true, true, "TEXT"), column("remarks", "Remarks", 22, 180, true, true, "TEXT"), column("status", "Status", 23, 90, true, true, "STATUS"), column("createdAt", "Created On", 24, 150, true, true, "DATETIME"));
    }

    private List<TableColumnConfig> dashboardContactType() {
        return summary("contactType", "Contact Type", "count", "Count", "percentage", "Percentage");
    }

    private List<TableColumnConfig> dashboardStatus() {
        return summary("status", "Status", "count", "Count", "percentage", "Percentage");
    }

    private List<TableColumnConfig> dashboardContactTypeStatus() {
        return List.of(column("contactType", "Contact Type", 1, 180, true, true, "TEXT"), column("active", "Active", 2, 100, true, true, "COUNT"), column("inactive", "Inactive", 3, 100, true, true, "COUNT"), column("total", "Total", 4, 100, true, true, "COUNT"));
    }

    private List<TableColumnConfig> dashboardContactTypeTrend() {
        return List.of(column("contactType", "Contact Type", 1, 180, true, true, "TEXT"), column("contacts", "Contacts", 2, 100, true, true, "COUNT"));
    }

    private List<TableColumnConfig> excelPreview() {
        return List.of(column("contactType", "Contact Type", 1, 145, true, false, "TEXT"), column("name", "Name", 2, 160, true, false, "TEXT"), column("mobile", "Mobile", 3, 135, true, false, "TEXT"), column("email", "Email", 4, 210, true, false, "TEXT"), column("department", "Department", 5, 145, true, false, "TEXT"), column("designation", "Designation", 6, 155, true, false, "TEXT"), column("companyName", "Company Name", 7, 175, true, false, "TEXT"), column("city", "City", 8, 125, true, false, "TEXT"), column("previousStatus", "Previous Status", 9, 135, true, false, "PREVIOUS_STATUS"), column("status", "New Status", 10, 120, true, false, "STATUS_TEXT"), column("action", "Action", 11, 110, true, false, "EXCEL_ACTION"), column("updateType", "Update Type", 12, 135, true, false, "TEXT"), column("changedFields", "Changed Fields", 13, 220, true, false, "TEXT"), column("error", "Result", 14, 220, true, false, "EXCEL_RESULT"));
    }

    private List<TableColumnConfig> summary(String firstKey, String firstHeader, String secondKey, String secondHeader, String thirdKey, String thirdHeader) {
        return List.of(column(firstKey, firstHeader, 1, 180, true, true, "TEXT"), column(secondKey, secondHeader, 2, 100, true, true, "COUNT"), column(thirdKey, thirdHeader, 3, 100, true, true, "PERCENT"));
    }

    private TableColumnConfig column(String key, String header, int order, int size, boolean visible, boolean sortable, String renderer) {
        return new TableColumnConfig(key, header, order, size, visible, sortable, renderer);
    }
}
