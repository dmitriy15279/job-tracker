package com.example.jobtracker.persistence;

import com.example.jobtracker.persistence.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
}
