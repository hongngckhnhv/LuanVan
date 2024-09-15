package com.example.demo.Repository;

import com.example.demo.Model.Categories;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoriesRepo extends JpaRepository<Categories, Integer> {
    Optional<Categories> findByCategoryId(Integer categoryId);
}
