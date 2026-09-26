package com.contactmanagement.master.config;

import com.contactmanagement.master.dto.MasterRequestDto;
import com.contactmanagement.master.enums.MasterType;
import com.contactmanagement.master.service.MasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;


@Configuration
@RequiredArgsConstructor
public class MasterDataInitializer {
    private final MasterService service;

    @Bean
    CommandLineRunner seedMasters() {
        return args -> {
            seed(MasterType.CONTACT_TYPE, List.of("Employee", "Vendor", "Customer", "Auditor", "Consultant"));
            seed(MasterType.MARITAL_STATUS, List.of("Married", "Unmarried"));
            seed(MasterType.BLOOD_GROUP, List.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"));
            seed(MasterType.LANGUAGE, List.of("Hindi", "English", "Punjabi", "Marathi", "Gujarati", "Bengali", "Tamil", "Telugu", "Kannada", "Malayalam", "Urdu"));
        };
    }

    private void seed(MasterType type, List<String> values) {
        values.forEach(name -> {
            MasterRequestDto request = new MasterRequestDto();
            request.setName(name);
            request.setStatus(false);
            service.save(type, request);
        });
    }
}
