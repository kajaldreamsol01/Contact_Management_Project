package com.contactmanagement.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OperationSummaryDto {
    private boolean update;
    private int totalCount;
    private int successCount;
    private int duplicateCount;
    private int invalidCount;
}
