package com.pms.repository;

import com.pms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                     USER REPOSITORY                                  ║
 * ║                                                                       ║
 * ║  SPRING DATA JPA REPOSITORY MAGIC:                                    ║
 * ║                                                                       ║
 * ║  JpaRepository<User, Long> gives us FREE implementations of:          ║
 * ║    save(entity)          → INSERT or UPDATE                           ║
 * ║    findById(id)          → SELECT WHERE id = ?                        ║
 * ║    findAll()             → SELECT *                                   ║
 * ║    delete(entity)        → DELETE WHERE id = ?                        ║
 * ║    count()               → SELECT COUNT(*)                            ║
 * ║    existsById(id)        → SELECT EXISTS(...)                         ║
 * ║    ... and 20+ more methods                                           ║
 * ║                                                                       ║
 * ║  We write the INTERFACE. Spring generates the IMPLEMENTATION.         ║
 * ║                                                                       ║
 * ║  @Repository                                                          ║
 * ║    → Marks as data access layer bean                                  ║
 * ║    → Spring translates SQL exceptions to DataAccessException          ║
 * ║    → Actually optional for JpaRepository (Spring Data detects it)    ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * SPRING DATA JPA METHOD NAMING CONVENTION:
     *
     * findBy + FieldName → Spring generates: SELECT * FROM users WHERE field_name = ?
     *
     * Method name = SQL query definition!
     * No SQL needed. Spring parses the method name.
     *
     * Optional<User> → Returns Optional to avoid NullPointerException
     * Always use Optional for queries that might return nothing.
     */
    Optional<User> findByEmail(String email);

    /**
     * Returns true if any user has this email.
     * Used in registration to check for duplicates.
     *
     * Generates: SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)
     */
    boolean existsByEmail(String email);

    /**
     * Soft delete query - only find non-deleted users
     * Generates: SELECT * FROM users WHERE email = ? AND is_deleted = false
     */
    Optional<User> findByEmailAndIsDeletedFalse(String email);
}
