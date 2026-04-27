package com.example.demo.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "pump_data")
@Data
public class PumpData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String scheme_name;
    private Double total_discharge;
    private Double each_discharge;
    private Double pump_head;
    private String quantity;
    private String pump_type;
}
