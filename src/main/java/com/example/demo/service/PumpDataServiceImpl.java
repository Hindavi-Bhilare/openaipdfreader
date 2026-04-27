package com.example.demo.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.entity.PumpData;
import com.example.demo.repo.PumpDataRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PumpDataServiceImpl {
	
	@Autowired
	private PumpDataRepository repository;

	public void savePumpData(String jsonResponse) throws Exception {

	    ObjectMapper mapper = new ObjectMapper();
	    List<Map<String, Object>> list = mapper.readValue(jsonResponse, List.class);

	    for (Map<String, Object> item : list) {
	        PumpData data = new PumpData();

	        data.setScheme_name((String) item.get("scheme_name"));
	        data.setTotal_discharge(Double.valueOf(item.get("total_discharge").toString()));
	        data.setEach_discharge(Double.valueOf(item.get("each_discharge").toString()));
	        data.setPump_head(Double.valueOf(item.get("pump_head").toString()));
	        data.setQuantity((String) item.get("quantity"));
	        data.setPump_type((String) item.get("pump_type"));

	        repository.save(data);
	    }
	}

}
