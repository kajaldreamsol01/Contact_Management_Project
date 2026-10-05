package com.contactmanagement.common.constants;

import com.contactmanagement.common.dto.CommonStatusCountDto;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonStatusCount {
    public static CommonStatusCountDto of(long active, long inactive) {
        return new CommonStatusCountDto(active, inactive);
    }
}
