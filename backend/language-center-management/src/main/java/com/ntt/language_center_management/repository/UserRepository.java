package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository
        extends JpaRepository<User, Integer>,
                JpaSpecificationExecutor<User> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    @Query("""
        select user
        from User user
        join user.roleId role
        where (:keyword is null
               or lower(user.username) like lower(concat('%', :keyword, '%'))
               or lower(user.fullName) like lower(concat('%', :keyword, '%'))
               or lower(user.email) like lower(concat('%', :keyword, '%')))
          and (:roleCode is null or upper(role.roleCode) = :roleCode)
          and (:status is null or upper(user.status) = :status)
        """)
    Page<User> searchAdminUsers(
        @Param("keyword") String keyword,
        @Param("roleCode") String roleCode,
        @Param("status") String status,
        Pageable pageable);
}
