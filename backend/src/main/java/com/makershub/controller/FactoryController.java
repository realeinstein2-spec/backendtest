package com.makershub.controller;

import com.makershub.dto.response.FactoryResponse;
import com.makershub.service.FactoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/factories")
@RequiredArgsConstructor
@Tag(name = "Factory Profiles")
public class FactoryController {

    private final FactoryService factoryService;

    @GetMapping
    @Operation(summary = "Get all factories",
               description = "Returns a paginated list of all active factory profiles in the database.")
    public ResponseEntity<org.springframework.data.domain.Page<FactoryResponse.FactoryPublicProfileResponse>> getAllFactories(
            @org.springframework.data.web.PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC)
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(factoryService.getAllFactories(pageable));
    }

    @GetMapping("/{factoryId}")
    @Operation(summary = "Get factory public profile by factory ID",
               description = "Returns all public factory details including owner information, sectors, machinery, ratings, and location.")
    public ResponseEntity<FactoryResponse.FactoryPublicProfileResponse> getFactoryProfile(@PathVariable UUID factoryId) {
        return ResponseEntity.ok(factoryService.getFactoryProfile(factoryId));
    }

    @GetMapping("/by-user/{userId}")
    @Operation(summary = "Get factory public profile by owner user ID",
               description = "Returns all public factory details for a given manufacturer/factory owner user ID.")
    public ResponseEntity<FactoryResponse.FactoryPublicProfileResponse> getFactoryProfileByUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(factoryService.getFactoryProfileByUserId(userId));
    }
}
