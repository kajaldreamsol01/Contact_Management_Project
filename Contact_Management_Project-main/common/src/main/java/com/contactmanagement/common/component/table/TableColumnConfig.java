package com.contactmanagement.common.component.table;

public record TableColumnConfig(String key, String header, int order, int size, boolean visible, boolean sortable,
                                String renderer) {
}