package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SystemLogRepository
    extends JpaRepository<SystemLog, Long>, JpaSpecificationExecutor<SystemLog> {}
