package com.contactmanagement.common.constants;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CacheConstants {
    public static final String CONTACT_GET_BY_ID = "contact:getById:", CONTACT_FILTER = "contact:filter:", CONTACT_FILTER_VERSION = "contact:filter:version", MASTER_DROPDOWNS = "master-dropdowns";

    public static String contactById(Long id) {
        return CONTACT_GET_BY_ID + id;
    }
}
