package com.contactmanagement.dto;

import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExistingLookupRequestDto {
    private Collection<Long> ids = List.of();
    private Collection<String> mobiles = List.of();
    private Collection<String> emails = List.of();

    public ExistingLookupRequestDto(Collection<String> mobiles, Collection<String> emails) {
        this(List.of(), mobiles, emails);
    }
}
