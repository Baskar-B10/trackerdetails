package com.example.trackerdetails.service;

import com.example.trackerdetails.model.Expenditure;
import com.example.trackerdetails.repository.ExpenditureRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExpenditureService {
    private final ExpenditureRepository repository;

    public ExpenditureService(ExpenditureRepository repository) {
        this.repository = repository;
    }

    public List<Expenditure> findAll() {
        return repository.findAll();
    }

    public List<Expenditure> findByYear(int year) {
        return repository.findByYear(year);
    }

    public Optional<Expenditure> findById(String id) {
        return repository.findById(id);
    }

    public Expenditure save(Expenditure exp) {
        return repository.save(exp);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }
}
