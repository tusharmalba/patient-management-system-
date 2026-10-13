package com.pms.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                    MEDICAL REPORT ENTITY                             ║
 * ║                                                                       ║
 * ║  Stores metadata about uploaded medical files (PDF/JPG/PNG).          ║
 * ║  Actual file bytes stored on filesystem, DB has the path/metadata.   ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 *
 * FILE STORAGE STRATEGY:
 * Option 1: Store in DB as BLOB (simple but slow, bloats DB)
 * Option 2: Store on filesystem, save path in DB (our approach)
 * Option 3: Cloud storage (S3, GCS) + save URL in DB (production best practice)
 *
 * We use Option 2 for simplicity. Production should use Option 3.
 */
@Entity
@Table(name = "medical_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class MedicalReport extends BaseEntity {

    /**
     * Link to patient this report belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    /**
     * User who uploaded this report (could be doctor or admin)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    /**
     * Original filename: "blood_test_march2024.pdf"
     */
    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    /**
     * Stored filename (UUID-based to avoid conflicts):
     * "a3f7b2c1-4d5e-6f78-9012-abcdef123456.pdf"
     */
    @Column(name = "stored_filename", nullable = false)
    private String storedFilename;

    /**
     * Filesystem path to the file:
     * "uploads/medical-reports/a3f7b2c1-4d5e-6f78-9012-abcdef123456.pdf"
     */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    /**
     * MIME type: "application/pdf", "image/jpeg", "image/png"
     * Used when serving the file back to client
     */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /**
     * File size in bytes
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * Human-readable description of the report
     */
    @Column(length = 500)
    private String description;

    /**
     * Type of report: "Blood Test", "X-Ray", "MRI", "Prescription"
     */
    @Column(name = "report_type", length = 100)
    private String reportType;
}
