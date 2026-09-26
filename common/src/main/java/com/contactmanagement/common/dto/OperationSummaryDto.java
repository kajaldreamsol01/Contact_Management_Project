package com.contactmanagement.common.dto;

import lombok.*;

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
