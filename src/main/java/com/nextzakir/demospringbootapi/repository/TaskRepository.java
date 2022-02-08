package com.nextzakir.demospringbootapi.repository;

import com.nextzakir.demospringbootapi.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @Query(value = "SELECT * FROM tasks WHERE task_title = ?1", nativeQuery = true)
    Optional<Task> findByTitle(String title);

    @Query(value = "SELECT * FROM tasks WHERE task_slug = ?1", nativeQuery = true)
    Optional<Task> findBySlug(String slug);

}