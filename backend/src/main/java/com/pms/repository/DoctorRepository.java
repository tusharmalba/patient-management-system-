package com.pms.repository;

import com.pms.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByEmailAndIsDeletedFalse(String email);
    boolean existsByEmail(String email);
    boolean existsByLicenseNumber(String licenseNumber);
    Page<Doctor> findByIsDeletedFalse(Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE " +
           "(LOWER(d.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(d.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND d.isDeleted = false")
    Page<Doctor> searchByName(@Param("name") String name, Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE " +
           "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :spec, '%')) " +
           "AND d.isDeleted = false")
    Page<Doctor> findBySpecialization(@Param("spec") String specialization, Pageable pageable);

    Optional<Doctor> findByUser_IdAndIsDeletedFalse(Long userId);

    long countByIsDeletedFalse();
}
