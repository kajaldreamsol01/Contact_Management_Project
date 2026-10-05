package com.contactmanagement.dto;

public record ContactFileResponseDto(String uuid, String fileName, String fileType) implements java.io.Serializable {
}
