package com.contactmanagement.repository;

import com.contactmanagement.entity.ContactHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactHistoryRepository extends JpaRepository<ContactHistory, Long> {
    List<ContactHistory> findByContact_IdOrderByCreatedAtDescIdDesc(Long contactId);
}
