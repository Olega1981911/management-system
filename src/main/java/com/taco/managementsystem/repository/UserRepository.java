package com.taco.managementsystem.repository;

import com.taco.managementsystem.model.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> userFindById(Long id);
    @Query("SELECT u FROM User u WHERE u.dateOfBirth > :date")
    Page<User> findByDateOfBirthAfter(@Param("date") LocalDate date, Pageable pageable);
    @Query("SELECT u FROM User u WHERE u.name LIKE CONCAT(:name, '%')")
    Page<User> findByNameStartingWith(@Param("name") String name, Pageable pageable);
    @Query("SELECT DISTINCT u FROM User u JOIN u.phones p WHERE p.phone = :phone")
    Page<User> findByPhone(@Param("phone") String phone, Pageable pageable);
    @Query("SELECT DISTINCT u FROM User u JOIN u.emails e WHERE e.email = :email")
    Page<User> findByEmail(@Param("email") String email, Pageable pageable);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
