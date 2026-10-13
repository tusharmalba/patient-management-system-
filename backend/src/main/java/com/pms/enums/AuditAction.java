package com.pms.enums;

/**
 * All trackable actions for Audit Logging.
 * Every important business event gets logged.
 */
public enum AuditAction {
    PATIENT_CREATED,
    PATIENT_UPDATED,
    PATIENT_DELETED,
    APPOINTMENT_CREATED,
    APPOINTMENT_UPDATED,
    APPOINTMENT_CANCELLED,
    DOCTOR_CREATED,
    DOCTOR_UPDATED,
    USER_REGISTERED,
    USER_LOGIN
}
