package ru.unirate.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.unirate.api.dto.MembershipDtos;
import ru.unirate.api.security.AuthUser;
import ru.unirate.application.MembershipService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/membership")
public class MembershipController {

    private final MembershipService membershipService;

    public MembershipController(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @GetMapping("/requests")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','STUDENT')")
    public List<MembershipDtos.MembershipRequestDto> requests(@AuthenticationPrincipal AuthUser viewer) {
        return membershipService.pendingRequests(viewer);
    }

    @PostMapping("/{studentUserId}/confirm")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','STUDENT')")
    public MembershipDtos.MembershipRequestDto confirm(@AuthenticationPrincipal AuthUser viewer,
                                                       @PathVariable Long studentUserId) {
        return membershipService.confirm(viewer, studentUserId);
    }

    @PostMapping("/{studentUserId}/reject")
    @PreAuthorize("hasAnyRole('TEACHER','ADMIN','STUDENT')")
    public MembershipDtos.MembershipRequestDto reject(@AuthenticationPrincipal AuthUser viewer,
                                                      @PathVariable Long studentUserId) {
        return membershipService.reject(viewer, studentUserId);
    }
}
