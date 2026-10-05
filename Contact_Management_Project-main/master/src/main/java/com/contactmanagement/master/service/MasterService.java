package com.contactmanagement.master.service;

import com.contactmanagement.common.constants.CacheConstants;
import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.common.util.PaginationUtil;
import com.contactmanagement.master.dto.MasterRequestDto;
import com.contactmanagement.master.dto.MasterResponseDto;
import com.contactmanagement.master.entity.MasterEntity;
import com.contactmanagement.master.enums.MasterType;
import com.contactmanagement.master.repository.MasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MasterService {
    private final MasterRepository repository;

    public ApiResponse<Page<MasterResponseDto>> list(MasterType type, Pageable pageable) {
        return ApiResponse.response("SUCCESS", "Master fetched successfully", repository.findAllByType(type, PaginationUtil.normalize(pageable, 10, 100)).map(this::map));
    }

    @Cacheable(cacheNames = CacheConstants.MASTER_DROPDOWNS, key = "'all'")
    public ApiResponse<Map<String, List<MasterResponseDto>>> dropdown() {
        Map<String, List<MasterResponseDto>> dropdownData = Arrays.stream(MasterType.values()).collect(Collectors.toMap(MasterType::dropdownKey, masterType -> repository.findAllByTypeAndStatusFalseOrderByNameAsc(masterType).stream().map(this::map).toList(), (existingValue, newValue) -> existingValue, LinkedHashMap::new));
        return ApiResponse.response("SUCCESS", "Master dropdown fetched successfully", dropdownData);
    }

    @CacheEvict(cacheNames = CacheConstants.MASTER_DROPDOWNS, allEntries = true)
    public ApiResponse<MasterResponseDto> save(MasterType type, MasterRequestDto request) {
        String masterName = Objects.toString(request.getName(), "").trim();
        if (masterName.isBlank()) return ApiResponse.response("FAILED", "Master name is required", null);
        MasterEntity masterEntity;
        if (Objects.isNull(request.getId())) {
            masterEntity = repository.findByTypeAndNameIgnoreCase(type, masterName).orElseGet(MasterEntity::new);
            masterEntity.setType(type);
        } else {
            masterEntity = repository.findByIdAndType(request.getId(), type).orElse(null);
            if (Objects.isNull(masterEntity)) return ApiResponse.response("FAILED", "Master not found", null);
            if (repository.existsByTypeAndNameIgnoreCaseAndIdNot(type, masterName, masterEntity.getId()))
                return ApiResponse.response("FAILED", "Master already exists", null);
        }
        masterEntity.setName(masterName);
        if (Objects.nonNull(request.getStatus())) masterEntity.setStatus(request.getStatus());
        else if (Objects.isNull(masterEntity.getId())) masterEntity.setStatus(false);
        return ApiResponse.response("SUCCESS", "Master saved successfully", map(repository.save(masterEntity)));
    }

    @CacheEvict(cacheNames = CacheConstants.MASTER_DROPDOWNS, allEntries = true)
    public ApiResponse<Void> delete(MasterType type, Long id) {
        MasterEntity masterEntity = repository.findByIdAndType(id, type).orElse(null);
        if (Objects.isNull(masterEntity)) return ApiResponse.response("FAILED", "Master not found", null);
        masterEntity.setStatus(true);
        repository.save(masterEntity);
        return ApiResponse.response("SUCCESS", "Master marked inactive successfully", null);
    }

    private MasterResponseDto map(MasterEntity masterEntity) {
        return new MasterResponseDto(masterEntity.getId(), masterEntity.getName(), masterEntity.isStatus());
    }
}