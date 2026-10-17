package com.example.demo.repository;

import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // CUSTOM QUERY 1: gaseste utilizatorii care trebuie notificati
    @Query("SELECT DISTINCT u FROM User u " +
            "JOIN UserSubscription us ON u.userId = us.user.userId " +
            "JOIN FireAlert fa ON us.region.regionId = fa.region.regionId " +
            "WHERE us.notifyByEmail = true AND fa.severityLevel = :severity")
    List<User> findUsersToNotifyForSeverity(@Param("severity") String severity);
}