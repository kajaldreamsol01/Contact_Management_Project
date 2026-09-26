package com.contactmanagement.common.component.excel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ExcelHeader {

    private ExcelHeader() {
    }

    public static final String[] HEADERS = {
            "Contact Code",
            "Contact Type",
            "Name",
            "Communication Name",
            "Department",
            "Designation",
            "Company Name",
            "Mobile",
            "Alternate Mobile",
            "Office Number",
            "Email",
            "Alternate Email",
            "Employee ID",
            "Gender",
            "Marital Status",
            "Date Of Birth",
            "Anniversary Date",
            "Blood Group",
            "Country",
            "State",
            "City",
            "Address",
            "Pin Code",
            "Skills",
            "Languages",
            "Emergency Contact Name",
            "Emergency Contact Number",
            "Remarks",
            "Status"
    };

    public static final List<String> CONTACT = List.of(HEADERS);

    public static final Set<Integer> REQUIRED = Set.of(
            1,
            2,
            7,
            10
    );

    public static final Map<String, String> UPLOAD_COLUMNS = uploadColumns();

    public static final Map<String, String> HEADER_MAP = headerMap();

    private static Map<String, String> uploadColumns() {
        Map<String, String> columns = new LinkedHashMap<>();

        columns.put("contactType", "Contact Type");
        columns.put("name", "Name");
        columns.put("mobile", "Mobile");
        columns.put("email", "Email");
        columns.put("department", "Department");
        columns.put("designation", "Designation");
        columns.put("companyName", "Company Name");
        columns.put("city", "City");

        return columns;
    }

    private static Map<String, String> headerMap() {
        Map<String, String> headers = new LinkedHashMap<>();

        headers.put("contact code", "contactCode");

        headers.put("contact type", "contactType");
        headers.put("contact category", "contactType");

        headers.put("name", "name");

        headers.put("communication name", "communicationName");

        headers.put("department", "department");

        headers.put("designation", "designation");

        headers.put("company name", "companyName");

        headers.put("mobile", "mobile");
        headers.put("mobile number", "mobile");

        headers.put("alternate mobile", "alternateMobile");

        headers.put("office number", "officeNumber");

        headers.put("email", "email");
        headers.put("email id", "email");

        headers.put("alternate email", "alternateEmail");

        headers.put("employee id", "employeeId");

        headers.put("gender", "gender");

        headers.put("marital status", "maritalStatus");

        headers.put("date of birth", "dateOfBirth");
        headers.put("dob", "dateOfBirth");

        headers.put("anniversary date", "anniversaryDate");
        headers.put("anniversary", "anniversaryDate");

        headers.put("blood group", "bloodGroup");

        headers.put("country", "country");

        headers.put("state", "state");

        headers.put("city", "city");

        headers.put("address", "address");

        headers.put("pin code", "pinCode");
        headers.put("pin-code", "pinCode");

        headers.put("skills", "skills");

        headers.put("languages", "languages");

        headers.put("emergency contact name", "emergencyContactName");

        headers.put("emergency contact number", "emergencyContactNumber");

        headers.put("remarks", "remarks");

        headers.put("status", "status");

        return headers;
    }
}