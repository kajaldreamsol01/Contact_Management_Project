package com.contactmanagement.controller;

import com.contactmanagement.common.component.table.ReactTableHeaderComponent;
import com.contactmanagement.common.component.table.TableColumnConfig;
import com.contactmanagement.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("contact/table-config")
@RequiredArgsConstructor
public class TableConfigController {

    private final ReactTableHeaderComponent tableHeader;

    @PreAuthorize("@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('hod') or @securityUtil.hasAuthority('management') or @securityUtil.hasAuthority('user')")
    @GetMapping("{table}")
    public ApiResponse<List<TableColumnConfig>> get(@PathVariable String table) {
        return ApiResponse.response("SUCCESS", "Table header fetched successfully", tableHeader.get(table));
    }
}