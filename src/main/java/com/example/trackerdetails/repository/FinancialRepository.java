package com.example.trackerdetails.repository;

import com.example.trackerdetails.model.FinancialEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialRepository extends MongoRepository<FinancialEntry, String> {
    List<FinancialEntry> findByYearAndType(int year, String type);
    List<FinancialEntry> findByYear(int year);
    Optional<FinancialEntry> findByYearAndMonthAndTypeAndCategory(int year, int month, String type, String category);
}
