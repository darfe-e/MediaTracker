package org.example.animetracker.repository;

import org.example.animetracker.model.SystemTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemTaskRepository extends JpaRepository<SystemTask, String> {
}