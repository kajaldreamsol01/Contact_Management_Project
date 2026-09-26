package com.contactmanagement.master.controller;

import com.contactmanagement.common.response.ApiResponse;
import com.contactmanagement.master.dto.MasterRequestDto;
import com.contactmanagement.master.dto.MasterResponseDto;
import com.contactmanagement.master.enums.MasterType;
import com.contactmanagement.master.service.MasterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/master")
@RequiredArgsConstructor
public class MasterController {
    private final MasterService service;

    @GetMapping("/dropdown")
    public ApiResponse<Map<String, List<MasterResponseDto>>> dropdown() {
        return service.dropdown();
    }

    @GetMapping("/{type}")
    public ApiResponse<Page<MasterResponseDto>> list(@PathVariable String type, Pageable pageable) {
        return service.list(MasterType.fromPath(type), pageable);
    }

    @PostMapping("/{type}")
    public ApiResponse<MasterResponseDto> save(@PathVariable String type, @Valid @RequestBody MasterRequestDto request) {
        request.setId(null);
        return service.save(MasterType.fromPath(type), request);
    }

    @PutMapping("/{type}/{id}")
    public ApiResponse<MasterResponseDto> update(@PathVariable String type, @PathVariable Long id, @Valid @RequestBody MasterRequestDto request) {
        request.setId(id);
        return service.save(MasterType.fromPath(type), request);
    }

    @DeleteMapping("/{type}/{id}")
    public ApiResponse<Void> delete(@PathVariable String type, @PathVariable Long id) {
        return service.delete(MasterType.fromPath(type), id);
    }
}
