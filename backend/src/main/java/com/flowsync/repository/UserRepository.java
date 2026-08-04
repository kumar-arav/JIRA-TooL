package com.flowsync.repository;

import com.flowsync.entity.User;
import com.flowsync.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findByActiveTrue();

    List<User> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
            String firstName,
            String lastName
    );

    default List<User> search(String q) {
        return findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(q, q);
    }
}