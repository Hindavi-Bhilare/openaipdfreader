package com.example.demo.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.PumpData;

public interface PumpDataRepository extends JpaRepository<PumpData, Integer> {
}
