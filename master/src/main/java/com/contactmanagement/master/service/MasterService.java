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

import java.util.*;
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
        Map<String, List<MasterResponseDto>> data = Arrays.stream(MasterType.values()).collect(Collectors.toMap(MasterType::dropdownKey, type -> repository.findAllByTypeAndStatusFalseOrderByNameAsc(type).stream().map(this::map).toList(), (a, b) -> a, LinkedHashMap::new));
        return ApiResponse.response("SUCCESS", "Master dropdown fetched successfully", data);
    }

    @CacheEvict(cacheNames = CacheConstants.MASTER_DROPDOWNS, allEntries = true)
    public ApiResponse<MasterResponseDto> save(MasterType type, MasterRequestDto request) {
        String name = Objects.toString(request.getName(), "").trim();
        if (name.isBlank()) return ApiResponse.response("FAILED", "Master name is required", null);
        MasterEntity entity;
        if (request.getId() == null) {
            entity = repository.findByTypeAndNameIgnoreCase(type, name).orElseGet(MasterEntity::new);
            entity.setType(type);
        } else {
            entity = repository.findByIdAndType(request.getId(), type).orElse(null);
            if (entity == null) return ApiResponse.response("FAILED", "Master not found", null);
            if (repository.existsByTypeAndNameIgnoreCaseAndIdNot(type, name, entity.getId()))
                return ApiResponse.response("FAILED", "Master already exists", null);
        }
        entity.setName(name);
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        else if (entity.getId() == null) entity.setStatus(false);
        return ApiResponse.response("SUCCESS", "Master saved successfully", map(repository.save(entity)));
    }

    @CacheEvict(cacheNames = CacheConstants.MASTER_DROPDOWNS, allEntries = true)
    public ApiResponse<Void> delete(MasterType type, Long id) {
        MasterEntity entity = repository.findByIdAndType(id, type).orElse(null);
        if (entity == null) return ApiResponse.response("FAILED", "Master not found", null);
        entity.setStatus(true);
        repository.save(entity);
        return ApiResponse.response("SUCCESS", "Master marked inactive successfully", null);
    }

    private MasterResponseDto map(MasterEntity entity) {
        return new MasterResponseDto(entity.getId(), entity.getName(), entity.isStatus());
    }
}
