package com.example.demo.repository;

import com.example.demo.entity.ApiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {

    // CUSTOM QUERY 2: Timpul mediu de răspuns al API-urilor
    @Query("SELECT a.httpStatus, AVG(a.responseTimeMs) FROM ApiLog a GROUP BY a.httpStatus")
    List<Object[]> findAverageResponseTimeByStatus();
}