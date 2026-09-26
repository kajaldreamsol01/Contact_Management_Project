package com.contactmanagement.master.enums;

import java.util.Arrays;

public enum MasterType {
    CONTACT_TYPE("contact-types", "contactTypes"), DEPARTMENT("departments", "departments"), CITY("cities", "cities"), GENDER("genders", "genders"), MARITAL_STATUS("marital-statuses", "maritalStatuses"), BLOOD_GROUP("blood-groups", "bloodGroups"), SKILL("skills", "skills"), LANGUAGE("languages", "languages");
    private final String path, dropdownKey;

    MasterType(String path, String dropdownKey) {
        this.path = path;
        this.dropdownKey = dropdownKey;
    }

    public String dropdownKey() {
        return dropdownKey;
    }

    public static MasterType fromPath(String path) {
        return Arrays.stream(values()).filter(v -> v.path.equalsIgnoreCase(path) || v.name().equalsIgnoreCase(path)).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid master type: " + path));
    }
}
