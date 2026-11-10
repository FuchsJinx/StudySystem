package ru.unirate.api;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.unirate.api.dto.CatalogDtos;
import ru.unirate.api.security.AuthUser;
import ru.unirate.application.CatalogService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/faculties")
    public List<CatalogDtos.FacultyDto> faculties() {
        return catalogService.faculties();
    }

    @GetMapping("/faculties/{id}/groups")
    public List<CatalogDtos.GroupDto> groups(@PathVariable Long id) {
        return catalogService.groupsByFaculty(id);
    }

    @GetMapping("/departments")
    public List<CatalogDtos.DepartmentDto> departments() {
        return catalogService.departments();
    }

    @GetMapping("/groups")
    public List<CatalogDtos.GroupDto> allGroups() {
        return catalogService.allGroups();
    }

    @GetMapping("/groups/my")
    public List<CatalogDtos.GroupDto> myGroups(@AuthenticationPrincipal AuthUser user) {
        return catalogService.myGroups(user);
    }
}
