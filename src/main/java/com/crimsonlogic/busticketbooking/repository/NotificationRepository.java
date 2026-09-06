package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(
            String userId
    );

    List<Notification> findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(
            String userId
    );
}
