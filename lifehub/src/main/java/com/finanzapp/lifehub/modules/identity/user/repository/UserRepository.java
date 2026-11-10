package com.finanzapp.lifehub.modules.identity.user.repository;

import com.finanzapp.lifehub.modules.identity.user.repository.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    List<UserEntity> findTop20ByEmailContainingIgnoreCaseOrNameContainingIgnoreCaseOrLastnameContainingIgnoreCase(
            String email, String name, String lastname);
}
