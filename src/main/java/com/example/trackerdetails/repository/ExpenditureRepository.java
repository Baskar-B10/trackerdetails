package com.example.trackerdetails.repository;

import com.example.trackerdetails.model.Expenditure;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenditureRepository extends MongoRepository<Expenditure, String> {
    List<Expenditure> findByYear(int year);
}
