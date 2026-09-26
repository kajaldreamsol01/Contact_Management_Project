package com.contactmanagement.common.repository;

import com.contactmanagement.common.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByIdAndDeletedFalse(Long id);

    Optional<Notification> findTopByActionTypeAndActionRequestIdAndAudienceEmailIgnoreCaseAndDeletedFalseOrderByIdDesc(
            String actionType,
            Long actionRequestId,
            String audienceEmail
    );

    @Query("""
            select n from Notification n
            where n.deleted = false
              and (
                    (n.audienceRole is null and n.audienceEmail is null)
                    or (:isAdmin = true and upper(n.audienceRole) = 'ADMIN')
                    or (n.audienceEmail is not null and lower(n.audienceEmail) = lower(:email))
                  )
            """)
    Page<Notification> findVisible(
            @Param("isAdmin") boolean isAdmin,
            @Param("email") String email,
            Pageable pageable
    );

    @Query("""
            select count(n) from Notification n
            where n.deleted = false
              and n.read = false
              and (
                    (n.audienceRole is null and n.audienceEmail is null)
                    or (:isAdmin = true and upper(n.audienceRole) = 'ADMIN')
                    or (n.audienceEmail is not null and lower(n.audienceEmail) = lower(:email))
                  )
            """)
    long countVisibleUnread(
            @Param("isAdmin") boolean isAdmin,
            @Param("email") String email
    );
}
