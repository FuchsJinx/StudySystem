package ru.unirate.api;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.unirate.api.dto.AdminDtos;
import ru.unirate.application.AdminService;
import ru.unirate.application.imports.StudentImportService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final StudentImportService importService;

    public AdminController(AdminService adminService, StudentImportService importService) {
        this.adminService = adminService;
        this.importService = importService;
    }

    @GetMapping("/stats")
    public AdminDtos.AdminStatsDto stats() {
        return adminService.stats();
    }

    // ---------- Faculties ----------

    @GetMapping("/faculties")
    public List<AdminDtos.FacultyAdminDto> faculties() {
        return adminService.faculties();
    }

    @PostMapping("/faculties")
    public ResponseEntity<AdminDtos.FacultyAdminDto> createFaculty(
            @Valid @RequestBody AdminDtos.NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createFaculty(request));
    }

    @PutMapping("/faculties/{id}")
    public AdminDtos.FacultyAdminDto updateFaculty(@PathVariable Long id,
                                                   @Valid @RequestBody AdminDtos.NameRequest request) {
        return adminService.updateFaculty(id, request);
    }

    @DeleteMapping("/faculties/{id}")
    public ResponseEntity<Void> deleteFaculty(@PathVariable Long id) {
        adminService.deleteFaculty(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Groups ----------

    @GetMapping("/groups")
    public List<AdminDtos.GroupAdminDto> groups() {
        return adminService.groups();
    }

    @PostMapping("/groups")
    public ResponseEntity<AdminDtos.GroupAdminDto> createGroup(
            @Valid @RequestBody AdminDtos.GroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createGroup(request));
    }

    @PutMapping("/groups/{id}")
    public AdminDtos.GroupAdminDto updateGroup(@PathVariable Long id,
                                               @Valid @RequestBody AdminDtos.GroupRequest request) {
        return adminService.updateGroup(id, request);
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        adminService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Departments ----------

    @GetMapping("/departments")
    public List<AdminDtos.DepartmentAdminDto> departments() {
        return adminService.departments();
    }

    @PostMapping("/departments")
    public ResponseEntity<AdminDtos.DepartmentAdminDto> createDepartment(
            @Valid @RequestBody AdminDtos.NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createDepartment(request));
    }

    @PutMapping("/departments/{id}")
    public AdminDtos.DepartmentAdminDto updateDepartment(@PathVariable Long id,
                                                         @Valid @RequestBody AdminDtos.NameRequest request) {
        return adminService.updateDepartment(id, request);
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        adminService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }

    // ---------- Import ----------

    @PostMapping("/import/students")
    public AdminDtos.ImportReport importStudents(@RequestPart("file") MultipartFile file) {
        return importService.importStudents(file);
    }

    // ---------- Settings ----------

    @GetMapping("/settings")
    public List<AdminDtos.SettingDto> settings() {
        return adminService.settings();
    }

    @PatchMapping("/settings")
    public List<AdminDtos.SettingDto> updateSettings(
            @Valid @RequestBody AdminDtos.SettingsUpdateRequest request) {
        return adminService.updateSettings(request);
    }

    // ---------- Semesters ----------

    @GetMapping("/semesters")
    public List<AdminDtos.SemesterDto> semesters() {
        return adminService.semesters();
    }

    @PostMapping("/semesters")
    public ResponseEntity<AdminDtos.SemesterDto> createSemester(
            @Valid @RequestBody AdminDtos.CreateSemesterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createSemester(request));
    }

    @PostMapping("/semesters/{id}/activate")
    public AdminDtos.SemesterActivationDto activateSemester(@PathVariable Long id) {
        return adminService.activateSemester(id);
    }

    // ---------- Users ----------

    @GetMapping("/users")
    public List<AdminDtos.AdminUserDto> users(@RequestParam(required = false) String role) {
        return adminService.users(role);
    }
}
