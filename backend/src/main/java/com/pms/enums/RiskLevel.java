package com.pms.enums;

/**
 * Patient risk assessment level.
 * Calculated dynamically from health factors:
 * age, diabetes, smoking, blood pressure, heart disease
 */
public enum RiskLevel {
    LOW,    // Score 0-3
    MEDIUM, // Score 4-6
    HIGH    // Score 7+
}
