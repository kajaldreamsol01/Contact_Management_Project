package com.contactmanagement.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collection;
import java.util.List;

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
