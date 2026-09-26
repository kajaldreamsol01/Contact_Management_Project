package com.contactmanagement.common.dto;

import com.contactmanagement.common.util.PaginationUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponsePageDto<T> implements Serializable {
    private List<T> data = List.of();
    private Map<String, Object> metaData;

    public ResponsePageDto(List<T> data) {
        this.data = data == null ? List.of() : data;
    }

    public ResponsePageDto(List<T> data, Map<String, Object> metaData) {
        this(data);
        this.metaData = metaData;
    }

    public static <T> ResponsePageDto<T> from(Page<T> page) {
        return page == null ? new ResponsePageDto<>() : new ResponsePageDto<>(page.getContent(), PaginationUtil.meta(page));
    }
}
