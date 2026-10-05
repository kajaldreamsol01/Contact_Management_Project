package com.contactmanagement.controller;

import com.contactmanagement.common.component.table.ReactTableHeaderComponent;
import com.contactmanagement.common.component.table.TableColumnConfig;
import com.contactmanagement.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("contact/table-headers")
@RequiredArgsConstructor
public class TableConfigController {
    private final ReactTableHeaderComponent tableHeader;

    @PreAuthorize("@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('hod') or @securityUtil.hasAuthority('management') or @securityUtil.hasAuthority('user')")
    @GetMapping("dashboard")
    public ApiResponse<Map<String, List<TableColumnConfig>>> dashboardHeaders() {
        return ApiResponse.response("SUCCESS", "Dashboard table headers fetched successfully", tableHeader.dashboard());
    }

    @PreAuthorize("@securityUtil.hasAuthority('admin') or @securityUtil.hasAuthority('hod') or @securityUtil.hasAuthority('management') or @securityUtil.hasAuthority('user')")
    @GetMapping("{table}")
    public ApiResponse<List<TableColumnConfig>> tableHeaders(@PathVariable String table) {
        return ApiResponse.response("SUCCESS", "Table headers fetched successfully", tableHeader.get(table));
    }
}
