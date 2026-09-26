package com.contactmanagement.service;

import com.contactmanagement.dto.ContactExistingResponseDto;
import com.contactmanagement.dto.ExistingLookupRequestDto;
import com.contactmanagement.repository.ContactRepository;
import com.contactmanagement.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ContactLookupService {
    private final ContactRepository repository;

    public ApiResponse<List<ContactExistingResponseDto>> existing(ExistingLookupRequestDto request) {
        Collection<Long> ids = Objects.isNull(request) || CollectionUtils.isEmpty(request.getIds())
                ? List.of(-1L)
                : request.getIds();
        Collection<String> mobiles = Objects.isNull(request) || CollectionUtils.isEmpty(request.getMobiles())
                ? List.of("__none__")
                : request.getMobiles();
        Collection<String> emails = Objects.isNull(request) || CollectionUtils.isEmpty(request.getEmails())
                ? List.of("__none__")
                : request.getEmails();

        return ApiResponse.response(
                "SUCCESS",
                "Existing contacts fetched successfully",
                repository.findExisting(ids, mobiles, emails)
        );
    }
}
