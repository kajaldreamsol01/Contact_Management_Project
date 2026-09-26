package com.contactmanagement.repository;

import com.contactmanagement.dto.ContactExistingResponseDto;
import com.contactmanagement.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long>, JpaSpecificationExecutor<Contact> {

    List<Contact> findTop10ByNameStartingWithIgnoreCaseOrderByNameAsc(String name);

    @Query("""
            SELECT new com.contactmanagement.dto.ContactExistingResponseDto(c)
            FROM Contact c
            WHERE c.id IN :ids
               OR c.contactCode IN :codes
               OR c.mobile IN :mobiles
               OR LOWER(c.email) IN :emails
            """)
    List<ContactExistingResponseDto> findExisting(
            @Param("ids") Collection<Long> ids,
            @Param("codes") Collection<String> codes,
            @Param("mobiles") Collection<String> mobiles,
            @Param("emails") Collection<String> emails
    );

    default List<ContactExistingResponseDto> findExisting(
            Collection<Long> ids,
            Collection<String> mobiles,
            Collection<String> emails
    ) {
        return findExisting(
                ids,
                List.of("__none__"),
                mobiles,
                emails
        );
    }
}