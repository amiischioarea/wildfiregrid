package com.example.demo.repository;

import com.example.demo.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // <- Ai nevoie de importul acesta pentru @Query
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List; // <- Ai nevoie de importul acesta pentru List<Object[]>

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    @Query(value = "SELECT r.country, COUNT(r) FROM Region r GROUP BY r.country")
    List<Object[]> countRegionsByCountry();

}