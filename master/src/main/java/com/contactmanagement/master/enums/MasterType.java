package com.contactmanagement.master.enums;

import java.util.Arrays;

public enum MasterType {
    CONTACT_TYPE("contact-types"), DEPARTMENT("departments"), CITY("cities"), GENDER("genders"), MARITAL_STATUS("marital-statuses"), BLOOD_GROUP("blood-groups"), SKILL("skills"), LANGUAGE("languages");
    private final String path;

    MasterType(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }

    public static MasterType fromPath(String path) {
        return Arrays.stream(values()).filter(v -> v.path.equalsIgnoreCase(path) || v.name().equalsIgnoreCase(path)).findFirst().orElseThrow(() -> new IllegalArgumentException("Invalid master type: " + path));
    }
}