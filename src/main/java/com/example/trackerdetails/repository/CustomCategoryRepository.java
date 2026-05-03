package com.example.trackerdetails.repository;

import com.example.trackerdetails.model.CustomCategory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomCategoryRepository extends MongoRepository<CustomCategory, String> {
    List<CustomCategory> findByTypeOrderBySortOrderAsc(String type);
    boolean existsByTypeAndName(String type, String name);
}
