package com.example.demo.repository;

import com.example.demo.entity.FireAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FireAlertRepository extends JpaRepository<FireAlert, Long> {
}