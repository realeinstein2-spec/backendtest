package com.makershub.service;

import com.makershub.dto.response.FactoryResponse;
import com.makershub.entity.Factory;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.mapper.DtoMapper;
import com.makershub.repository.FactoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FactoryService {

    private final FactoryRepository factoryRepository;
    private final DtoMapper mapper;

    /**
     * Returns the full public profile for a factory by its ID.
     * Combines factory details with public owner (manufacturer) information.
     */
    @Transactional(readOnly = true)
    public FactoryResponse.FactoryPublicProfileResponse getFactoryProfile(UUID factoryId) {
        Factory factory = factoryRepository.findByIdAndDeletedAtIsNull(factoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Factory", factoryId.toString()));
        return mapper.toFactoryPublicProfile(factory);
    }

    /**
     * Returns the full public profile for a factory by its owner's user ID.
     */
    @Transactional(readOnly = true)
    public FactoryResponse.FactoryPublicProfileResponse getFactoryProfileByUserId(UUID userId) {
        Factory factory = factoryRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Factory", "User ID: " + userId));
        return mapper.toFactoryPublicProfile(factory);
    }

    /**
     * Returns a paginated list of all active factories in the database.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<FactoryResponse.FactoryPublicProfileResponse> getAllFactories(org.springframework.data.domain.Pageable pageable) {
        return factoryRepository.findAllByDeletedAtIsNull(pageable)
                .map(mapper::toFactoryPublicProfile);
    }
}
