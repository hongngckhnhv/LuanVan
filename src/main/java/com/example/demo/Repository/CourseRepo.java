package com.example.demo.Repository;

import com.example.demo.Model.Courses;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepo extends JpaRepository<Courses, Integer> {
    Optional<Courses> findByWebCourseId(Integer webCourseId);
}
