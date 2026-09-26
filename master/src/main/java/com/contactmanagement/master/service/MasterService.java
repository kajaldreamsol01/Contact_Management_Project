package com.contactmanagement.master.service;

import com.contactmanagement.master.dto.MasterRequestDto;
import com.contactmanagement.master.dto.MasterResponseDto;
import com.contactmanagement.master.entity.MasterEntity;
import com.contactmanagement.master.enums.MasterType;
import com.contactmanagement.master.repository.MasterRepository;
import com.contactmanagement.master.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MasterService {

    private final MasterRepository repository;

    public ApiResponse<Page<MasterResponseDto>> list(
            MasterType type,
            Pageable pageable
    ) {
        return ApiResponse.response(
                "SUCCESS",
                "Master fetched successfully",
                repository.findAllByType(type, pageable).map(this::map)
        );
    }

    @Cacheable(cacheNames = "master-active", key = "#type.name()")
    public ApiResponse<List<MasterResponseDto>> active(MasterType type) {
        return ApiResponse.response(
                "SUCCESS",
                "Dropdown fetched successfully",
                activeData(type)
        );
    }

    @Cacheable(cacheNames = "master-dropdowns", key = "'all'")
    public ApiResponse<Map<String, List<MasterResponseDto>>> dropdown() {
        Map<String, List<MasterResponseDto>> data = new LinkedHashMap<>();

        data.put("contactTypes", dropdownData(MasterType.CONTACT_TYPE));
        data.put("departments", dropdownData(MasterType.DEPARTMENT));
        data.put("cities", dropdownData(MasterType.CITY));
        data.put("genders", dropdownData(MasterType.GENDER));
        data.put("maritalStatuses", dropdownData(MasterType.MARITAL_STATUS));
        data.put("bloodGroups", dropdownData(MasterType.BLOOD_GROUP));
        data.put("skills", dropdownData(MasterType.SKILL));
        data.put("languages", dropdownData(MasterType.LANGUAGE));

        return ApiResponse.response(
                "SUCCESS",
                "Master dropdown fetched successfully",
                data
        );
    }

    @Caching(evict = {
            @CacheEvict(
                    cacheNames = "master-active",
                    key = "#type.name()"
            ),
            @CacheEvict(
                    cacheNames = "master-dropdowns",
                    allEntries = true
            )
    })
    public ApiResponse<MasterResponseDto> save(
            MasterType type,
            MasterRequestDto request
    ) {
        String name = Objects.toString(request.getName(), "").trim();

        if (name.isBlank()) {
            return ApiResponse.response(
                    "FAILED",
                    "Master name is required",
                    null
            );
        }

        MasterEntity entity;

        if (Objects.isNull(request.getId())) {
            entity = repository
                    .findByTypeAndNameIgnoreCase(type, name)
                    .orElseGet(MasterEntity::new);

            entity.setType(type);
        } else {
            entity = repository
                    .findByIdAndType(request.getId(), type)
                    .orElse(null);

            if (Objects.isNull(entity)) {
                return ApiResponse.response(
                        "FAILED",
                        "Master not found",
                        null
                );
            }

            if (repository.existsByTypeAndNameIgnoreCaseAndIdNot(
                    type,
                    name,
                    entity.getId()
            )) {
                return ApiResponse.response(
                        "FAILED",
                        "Master already exists",
                        null
                );
            }
        }

        entity.setName(name);

        if (Objects.nonNull(request.getStatus())) {
            entity.setStatus(request.getStatus());
        } else if (Objects.isNull(entity.getId())) {
            entity.setStatus(false);
        }

        return ApiResponse.response(
                "SUCCESS",
                "Master saved successfully",
                map(repository.save(entity))
        );
    }

    @Caching(evict = {
            @CacheEvict(
                    cacheNames = "master-active",
                    key = "#type.name()"
            ),
            @CacheEvict(
                    cacheNames = "master-dropdowns",
                    allEntries = true
            )
    })
    public ApiResponse<Void> delete(
            MasterType type,
            Long id
    ) {
        MasterEntity entity = repository
                .findByIdAndType(id, type)
                .orElse(null);

        if (Objects.isNull(entity)) {
            return ApiResponse.response(
                    "FAILED",
                    "Master not found",
                    null
            );
        }

        entity.setStatus(true);
        repository.save(entity);

        return ApiResponse.response(
                "SUCCESS",
                "Master marked inactive successfully",
                null
        );
    }

    public List<MasterResponseDto> activeData(MasterType type) {
        return repository
                .findAllByTypeAndStatusFalseOrderByNameAsc(type)
                .stream()
                .map(this::map)
                .toList();
    }

    private List<MasterResponseDto> dropdownData(MasterType type) {
        return repository
                .findAllByTypeOrderByNameAsc(type)
                .stream()
                .map(this::map)
                .toList();
    }

    private MasterResponseDto map(MasterEntity entity) {
        return new MasterResponseDto(
                entity.getId(),
                entity.getName(),
                entity.isStatus()
        );
    }
}