package com.pms.service;

import com.pms.audit.AuditService;
import com.pms.dto.request.DoctorRequest;
import com.pms.dto.response.DoctorResponse;
import com.pms.entity.Doctor;
import com.pms.entity.User;
import com.pms.enums.AuditAction;
import com.pms.enums.Role;
import com.pms.exception.DuplicateResourceException;
import com.pms.exception.ResourceNotFoundException;
import com.pms.repository.DoctorRepository;
import com.pms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request) {
        if (doctorRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Doctor", "email", request.getEmail());
        }
        if (request.getLicenseNumber() != null
                && doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new DuplicateResourceException("Doctor", "licenseNumber", request.getLicenseNumber());
        }

        Doctor doctor = Doctor.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .specialization(request.getSpecialization())
                .licenseNumber(request.getLicenseNumber())
                .experienceYears(request.getExperienceYears())
                .qualification(request.getQualification())
                .bio(request.getBio())
                .consultationFee(request.getConsultationFee())
                .availableDays(request.getAvailableDays())
                .availableFrom(request.getAvailableFrom())
                .availableTo(request.getAvailableTo())
                .appointmentDurationMinutes(
                        request.getAppointmentDurationMinutes() != null
                                ? request.getAppointmentDurationMinutes() : 30)
                .build();

        // Create user account for doctor login
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            User user = User.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.DOCTOR)
                    .phoneNumber(request.getPhoneNumber())
                    .enabled(true)
                    .build();
            doctor.setUser(userRepository.save(user));
        }

        Doctor saved = doctorRepository.save(doctor);
        auditService.log(AuditAction.DOCTOR_CREATED, "Doctor", saved.getId(),
                "Doctor '" + saved.getFullName() + "' created, specialization: " + saved.getSpecialization());

        return mapToResponse(saved);
    }

    @Transactional
    public DoctorResponse updateDoctor(Long id, DoctorRequest request) {
        Doctor doctor = getActiveDoctorById(id);

        if (!doctor.getEmail().equals(request.getEmail())
                && doctorRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Doctor", "email", request.getEmail());
        }

        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setEmail(request.getEmail());
        doctor.setPhoneNumber(request.getPhoneNumber());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setExperienceYears(request.getExperienceYears());
        doctor.setQualification(request.getQualification());
        doctor.setBio(request.getBio());
        doctor.setConsultationFee(request.getConsultationFee());
        doctor.setAvailableDays(request.getAvailableDays());
        doctor.setAvailableFrom(request.getAvailableFrom());
        doctor.setAvailableTo(request.getAvailableTo());
        if (request.getAppointmentDurationMinutes() != null)
            doctor.setAppointmentDurationMinutes(request.getAppointmentDurationMinutes());

        auditService.log(AuditAction.DOCTOR_UPDATED, "Doctor", doctor.getId(),
                "Doctor '" + doctor.getFullName() + "' updated");

        return mapToResponse(doctorRepository.save(doctor));
    }

    @Transactional
    public void deleteDoctor(Long id) {
        Doctor doctor = getActiveDoctorById(id);
        doctor.softDelete();
        doctorRepository.save(doctor);
        log.info("Doctor soft-deleted: {}", doctor.getFullName());
    }

    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(Long id) {
        return mapToResponse(getActiveDoctorById(id));
    }

    @Transactional(readOnly = true)
    public Page<DoctorResponse> getAllDoctors(Pageable pageable) {
        return doctorRepository.findByIsDeletedFalse(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<DoctorResponse> searchDoctors(String name, Pageable pageable) {
        return doctorRepository.searchByName(name, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<DoctorResponse> getDoctorsBySpecialization(String spec, Pageable pageable) {
        return doctorRepository.findBySpecialization(spec, pageable).map(this::mapToResponse);
    }

    private Doctor getActiveDoctorById(Long id) {
        return doctorRepository.findById(id)
                .filter(Doctor::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", "id", id));
    }

    private DoctorResponse mapToResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .fullName(doctor.getFullName())
                .email(doctor.getEmail())
                .phoneNumber(doctor.getPhoneNumber())
                .specialization(doctor.getSpecialization())
                .licenseNumber(doctor.getLicenseNumber())
                .experienceYears(doctor.getExperienceYears())
                .qualification(doctor.getQualification())
                .bio(doctor.getBio())
                .consultationFee(doctor.getConsultationFee())
                .availableDays(doctor.getAvailableDays())
                .availableFrom(doctor.getAvailableFrom())
                .availableTo(doctor.getAvailableTo())
                .appointmentDurationMinutes(doctor.getAppointmentDurationMinutes())
                .createdAt(doctor.getCreatedAt())
                .userId(doctor.getUser() != null ? doctor.getUser().getId() : null)
                .build();
    }
}
