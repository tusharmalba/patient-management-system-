package com.pms.enums;

/**
 * ╔══════════════════════════════════════════════════════╗
 * ║                    ROLE ENUM                         ║
 * ║  Defines user roles for Spring Security              ║
 * ╚══════════════════════════════════════════════════════╝
 *
 * Spring Security uses ROLE_ prefix convention.
 * When we say hasRole("ADMIN"), Spring checks for "ROLE_ADMIN"
 * But we store just "ADMIN" in DB and add ROLE_ prefix in code.
 */
public enum Role {
    ADMIN,    // Full access - manage doctors, patients, settings
    DOCTOR,   // Can view/manage their patients and appointments
    PATIENT   // Can view own data and book appointments
}
