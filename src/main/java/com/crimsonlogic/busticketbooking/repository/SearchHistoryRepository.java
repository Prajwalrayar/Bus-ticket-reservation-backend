package com.crimsonlogic.busticketbooking.repository;

import com.crimsonlogic.busticketbooking.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, String> {

    List<SearchHistory> findByUser_UserId(String userId);
}
