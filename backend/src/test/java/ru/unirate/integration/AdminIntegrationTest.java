package ru.unirate.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.unirate.infrastructure.persistence.entity.StudentProfile;
import ru.unirate.infrastructure.persistence.repo.FacultyRepository;
import ru.unirate.infrastructure.persistence.repo.StudentGroupRepository;
import ru.unirate.infrastructure.persistence.repo.StudentProfileRepository;
import ru.unirate.infrastructure.persistence.repo.UserRepository;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminIntegrationTest extends IntegrationTestBase {

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentGroupRepository groupRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    private long createFaculty(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/faculties")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return json(result).get("id").asLong();
    }

    @Test
    void onlyAdminCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/stats"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + teacherToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + studentToken()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.students").isNumber());
    }

    @Test
    void catalogCrudAndDeletionGuards() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        long facultyId = createFaculty("Тест-факультет-" + suffix);
        MvcResult groupResult = mockMvc.perform(post("/api/v1/admin/groups")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "ТГ-" + suffix, "facultyId", facultyId))))
                .andExpect(status().isCreated())
                .andReturn();
        long groupId = json(groupResult).get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/groups/" + groupId)
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "ТГ2-" + suffix, "facultyId", facultyId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ТГ2-" + suffix));

        mockMvc.perform(delete("/api/v1/admin/groups/" + groupId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/admin/faculties/" + facultyId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/v1/admin/faculties/1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/v1/admin/departments/1")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isConflict());

        long departmentId = json(mockMvc.perform(post("/api/v1/admin/departments")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Кафедра-" + suffix))))
                .andExpect(status().isCreated())
                .andReturn()).get("id").asLong();
        mockMvc.perform(delete("/api/v1/admin/departments/" + departmentId)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/admin/faculties")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "ФИТ"))))
                .andExpect(status().isConflict());
    }

    @Test
    void settingsCanBeReadAndUpdated() throws Exception {
        JsonNode settings = getJson("/api/v1/admin/settings", adminToken());
        assertThat(settings.size()).isGreaterThanOrEqualTo(7);
        boolean hasAmount = false;
        for (JsonNode item : settings) {
            if (item.get("key").asText().equals("arDeductAmount")) {
                hasAmount = true;
                assertThat(item.get("value").asText()).isEqualTo("20");
            }
        }
        assertThat(hasAmount).isTrue();

        mockMvc.perform(patch("/api/v1/admin/settings")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "values", Map.of("arDeductAmount", "15")))))
                .andExpect(status().isOk());

        JsonNode updated = getJson("/api/v1/admin/settings", adminToken());
        for (JsonNode item : updated) {
            if (item.get("key").asText().equals("arDeductAmount")) {
                assertThat(item.get("value").asText()).isEqualTo("15");
            }
        }

        mockMvc.perform(patch("/api/v1/admin/settings")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "values", Map.of("arDeductAmount", "0")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SETTING_INVALID"));

        mockMvc.perform(patch("/api/v1/admin/settings")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "values", Map.of("unknownSetting", "1")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void semesterActivationAddsBonusOnceWithCap() throws Exception {
        StudentProfile student = studentProfileRepository.findByUserId(4L).orElseThrow();
        student.setAcademicRating(900);
        studentProfileRepository.save(student);

        MvcResult created = mockMvc.perform(post("/api/v1/admin/semesters")
                        .header("Authorization", "Bearer " + adminToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Тестовый семестр " + UUID.randomUUID(),
                                "startAt", LocalDate.now().toString(),
                                "endAt", LocalDate.now().plusMonths(4).toString()))))
                .andExpect(status().isCreated())
                .andReturn();
        long semesterId = json(created).get("id").asLong();

        mockMvc.perform(post("/api/v1/admin/semesters/" + semesterId + "/activate")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.affectedStudents").value(1))
                .andExpect(jsonPath("$.totalBonus").value(100))
                .andExpect(jsonPath("$.semester.active").value(true));

        StudentProfile updated = studentProfileRepository.findByUserId(4L).orElseThrow();
        assertThat(updated.getAcademicRating()).isEqualTo(1000);

        mockMvc.perform(post("/api/v1/admin/semesters/" + semesterId + "/activate")
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SEMESTER_ALREADY_ACTIVATED"));
    }

    @Test
    void usersListCanBeFilteredByRole() throws Exception {
        JsonNode students = getJson("/api/v1/admin/users?role=STUDENT", adminToken());
        assertThat(students.size()).isGreaterThanOrEqualTo(5);
        for (JsonNode student : students) {
            assertThat(student.get("role").asText()).isEqualTo("STUDENT");
            assertThat(student.get("membershipStatus").asText()).isNotBlank();
        }
        JsonNode teachers = getJson("/api/v1/admin/users?role=TEACHER", adminToken());
        assertThat(teachers.size()).isGreaterThanOrEqualTo(2);
        assertThat(teachers.get(0).get("departmentName").asText()).isNotBlank();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void csvImportCreatesStudentsAndReportsBadRows() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "import-" + suffix + "@test.local";
        String facultyName = "Импорт-факультет-" + suffix;
        String groupName = "ИМП-" + suffix;
        String csv = """
                email,fullName,facultyName,groupName,passwordOptional
                %s,Импортов Иван,%s,%s,
                %s,Дубликатов Иван,%s,%s,
                bad-email,Ошибочный,%s,%s,
                %s,,%s,%s,
                """.formatted(email, facultyName, groupName, email, facultyName, groupName,
                facultyName, groupName, "missing-name-" + suffix + "@test.local", facultyName, groupName);

        MockMultipartFile file = new MockMultipartFile(
                "file", "students.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));
        try {
            MvcResult result = mockMvc.perform(multipart("/api/v1/admin/import/students")
                            .file(file)
                            .header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode report = json(result);
            assertThat(report.get("created").asInt()).isEqualTo(1);
            assertThat(report.get("skipped").asInt()).isEqualTo(1);
            assertThat(report.get("errors").size()).isEqualTo(2);

            JsonNode me = getJson("/api/v1/auth/me", tokenFor(email, "Demo123!"));
            assertThat(me.get("student").get("membershipStatus").asText()).isEqualTo("CONFIRMED");
            assertThat(me.get("student").get("groupName").asText()).isEqualTo(groupName);
            assertThat(me.get("emailVerified").asBoolean()).isTrue();
        } finally {
            userRepository.findByEmailIgnoreCase(email).ifPresent(userRepository::delete);
            facultyRepository.findByNameIgnoreCase(facultyName).ifPresent(faculty ->
                    groupRepository.findByFacultyIdAndNameIgnoreCase(faculty.getId(), groupName)
                            .ifPresent(groupRepository::delete));
            facultyRepository.findByNameIgnoreCase(facultyName).ifPresent(facultyRepository::delete);
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void excelImportCreatesStudent() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "excel-" + suffix + "@test.local";
        String facultyName = "Excel-факультет-" + suffix;
        String groupName = "XL-" + suffix;
        byte[] workbookBytes;
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Студенты");
            Row header = sheet.createRow(0);
            String[] headers = {"email", "fullName", "facultyName", "groupName", "passwordOptional"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue(email);
            row.createCell(1).setCellValue("Экселев Иван");
            row.createCell(2).setCellValue(facultyName);
            row.createCell(3).setCellValue(groupName);
            row.createCell(4).setCellValue("");
            workbook.write(output);
            workbookBytes = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "students.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", workbookBytes);
        try {
            mockMvc.perform(multipart("/api/v1/admin/import/students")
                            .file(file)
                            .header("Authorization", "Bearer " + adminToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.created").value(1))
                    .andExpect(jsonPath("$.errors").isEmpty());

            JsonNode me = getJson("/api/v1/auth/me", tokenFor(email, "Demo123!"));
            assertThat(me.get("student").get("groupName").asText()).isEqualTo(groupName);
        } finally {
            userRepository.findByEmailIgnoreCase(email).ifPresent(userRepository::delete);
            facultyRepository.findByNameIgnoreCase(facultyName).ifPresent(faculty ->
                    groupRepository.findByFacultyIdAndNameIgnoreCase(faculty.getId(), groupName)
                            .ifPresent(groupRepository::delete));
            facultyRepository.findByNameIgnoreCase(facultyName).ifPresent(facultyRepository::delete);
        }
    }

    @Test
    void importRejectsUnsupportedFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "students.txt", "text/plain", "email".getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart("/api/v1/admin/import/students")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("IMPORT_INVALID_FILE"));
    }
}
