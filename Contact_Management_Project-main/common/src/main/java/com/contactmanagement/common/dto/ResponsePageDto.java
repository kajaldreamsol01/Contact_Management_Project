package com.contactmanagement.common.dto;

import com.contactmanagement.common.util.PaginationUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResponsePageDto<T> implements Serializable {
    private List<T> data = List.of();
    private Map<String, Object> metaData;

    public ResponsePageDto(List<T> data) {
        this.data = Objects.isNull(data) ? List.of() : data;
    }

    public ResponsePageDto(List<T> data, Map<String, Object> metaData) {
        this(data);
        this.metaData = metaData;
    }

    public static <T> ResponsePageDto<T> from(Page<T> page) {
        return Objects.isNull(page) ? new ResponsePageDto<>() : new ResponsePageDto<>(page.getContent(), PaginationUtil.meta(page));
    }
}
