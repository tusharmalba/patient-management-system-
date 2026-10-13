package com.pms.service;

import com.pms.entity.MedicalReport;
import com.pms.entity.Patient;
import com.pms.entity.User;
import com.pms.exception.FileStorageException;
import com.pms.exception.ResourceNotFoundException;
import com.pms.repository.MedicalReportRepository;
import com.pms.repository.PatientRepository;
import com.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 * ║                   FILE STORAGE SERVICE                               ║
 * ║                                                                       ║
 * ║  Handles medical report file uploads.                                 ║
 * ║                                                                       ║
 * ║  STRATEGY:                                                            ║
 * ║  - Generate UUID filename (prevents overwrite collisions)             ║
 * ║  - Store file on filesystem (uploads/medical-reports/)                ║
 * ║  - Save metadata (path, type, size) in DB                             ║
 * ║  - Client uses /api/reports/{id}/download to retrieve file            ║
 * ║                                                                       ║
 * ║  PRODUCTION NOTE:                                                     ║
 * ║  Replace filesystem storage with AWS S3 / Google Cloud Storage        ║
 * ║  for scalable, persistent, distributed file storage.                  ║
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {

    private final MedicalReportRepository medicalReportRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    // Allowed file types for medical reports
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/jpg"
    );

    /**
     * Upload a medical report file for a patient.
     *
     * @param patientId     Patient this report belongs to
     * @param file          The uploaded file (MultipartFile from HTTP multipart request)
     * @param description   Human-readable description
     * @param reportType    "Blood Test", "X-Ray", "MRI", etc.
     * @param uploaderEmail Email of the person uploading (from JWT)
     */
    @Transactional
    public MedicalReport uploadReport(Long patientId, MultipartFile file,
                                       String description, String reportType,
                                       String uploaderEmail) {
        // ── Validate patient exists ──
        Patient patient = patientRepository.findById(patientId)
                .filter(Patient::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", "id", patientId));

        // ── Validate file type ──
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new FileStorageException(
                    "Invalid file type. Allowed types: PDF, JPG, PNG. Got: " + contentType);
        }

        // ── Validate file is not empty ──
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot store empty file");
        }

        // ── Generate unique filename ──
        String originalFilename = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file"
        );
        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";

        // UUID prevents filename collisions (two patients uploading "report.pdf")
        String storedFilename = UUID.randomUUID().toString() + extension;

        // ── Create upload directory if it doesn't exist ──
        Path uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory", e);
        }

        // ── Copy file to filesystem ──
        Path filePath = uploadPath.resolve(storedFilename);
        try {
            // StandardCopyOption.REPLACE_EXISTING → Replace if file somehow already exists
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + originalFilename, e);
        }

        // ── Find uploader user ──
        User uploader = userRepository.findByEmailAndIsDeletedFalse(uploaderEmail).orElse(null);

        // ── Save metadata to database ──
        MedicalReport report = MedicalReport.builder()
                .patient(patient)
                .uploadedBy(uploader)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .filePath(filePath.toString())
                .contentType(contentType)
                .fileSize(file.getSize())
                .description(description)
                .reportType(reportType)
                .build();

        MedicalReport saved = medicalReportRepository.save(report);
        log.info("Medical report uploaded: {} for patient ID: {}", storedFilename, patientId);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<MedicalReport> getPatientReports(Long patientId) {
        return medicalReportRepository.findByPatient_IdAndIsDeletedFalse(patientId);
    }

    /**
     * Get the filesystem Path for a report file (for download streaming).
     */
    @Transactional(readOnly = true)
    public Path getReportFilePath(Long reportId) {
        MedicalReport report = medicalReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
        Path path = Paths.get(report.getFilePath());
        if (!Files.exists(path)) {
            throw new FileStorageException("File not found on server: " + report.getStoredFilename());
        }
        return path;
    }

    @Transactional(readOnly = true)
    public MedicalReport getReportMetadata(Long reportId) {
        return medicalReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
    }
}
