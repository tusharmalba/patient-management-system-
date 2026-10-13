package com.pms.enums;

/**
 * Lifecycle of an appointment.
 *
 * Flow: SCHEDULED → CONFIRMED → COMPLETED
 *                             → CANCELLED
 *                             → NO_SHOW
 */
public enum AppointmentStatus {
    SCHEDULED,   // Initial state when booked
    CONFIRMED,   // Doctor confirmed
    COMPLETED,   // Appointment done
    CANCELLED,   // Cancelled by patient or doctor
    NO_SHOW      // Patient didn't show up
}
