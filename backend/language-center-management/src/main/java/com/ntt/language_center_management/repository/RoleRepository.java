package com.ntt.language_center_management.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ntt.language_center_management.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByRoleCodeIgnoreCase(String roleCode);
}
