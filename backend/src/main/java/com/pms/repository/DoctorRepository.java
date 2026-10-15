package com.pms.repository;

import com.pms.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Same NULL-safety fix as PatientRepository.
 * All queries: (isDeleted = false OR isDeleted IS NULL)
 */
@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Query("SELECT d FROM Doctor d WHERE d.email = :email AND (d.isDeleted = false OR d.isDeleted IS NULL)")
    Optional<Doctor> findByEmailAndIsDeletedFalse(@Param("email") String email);

    boolean existsByEmail(String email);
    boolean existsByLicenseNumber(String licenseNumber);

    @Query("SELECT d FROM Doctor d WHERE (d.isDeleted = false OR d.isDeleted IS NULL)")
    Page<Doctor> findByIsDeletedFalse(Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE " +
           "(LOWER(d.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(d.lastName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (d.isDeleted = false OR d.isDeleted IS NULL)")
    Page<Doctor> searchByName(@Param("name") String name, Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE " +
           "LOWER(d.specialization) LIKE LOWER(CONCAT('%', :spec, '%')) " +
           "AND (d.isDeleted = false OR d.isDeleted IS NULL)")
    Page<Doctor> findBySpecialization(@Param("spec") String specialization, Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE d.user.id = :userId AND (d.isDeleted = false OR d.isDeleted IS NULL)")
    Optional<Doctor> findByUser_IdAndIsDeletedFalse(@Param("userId") Long userId);

    @Query("SELECT COUNT(d) FROM Doctor d WHERE (d.isDeleted = false OR d.isDeleted IS NULL)")
    long countByIsDeletedFalse();

    @Modifying
    @Query("UPDATE Doctor d SET d.isDeleted = false WHERE d.isDeleted IS NULL")
    int fixNullIsDeletedRecords();
}
