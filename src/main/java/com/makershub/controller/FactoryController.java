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
