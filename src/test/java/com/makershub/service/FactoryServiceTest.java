package com.makershub.service;

import com.makershub.dto.response.FactoryResponse;
import com.makershub.entity.Factory;
import com.makershub.entity.User;
import com.makershub.enums.UserRole;
import com.makershub.enums.VerificationStatus;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.mapper.DtoMapper;
import com.makershub.repository.FactoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FactoryServiceTest {

    @Mock
    private FactoryRepository factoryRepository;

    @Mock
    private DtoMapper mapper;

    @InjectMocks
    private FactoryService factoryService;

    private Factory testFactory;
    private User testUser;
    private UUID factoryId;
    private UUID userId;
    private FactoryResponse.FactoryPublicProfileResponse expectedResponse;

    @BeforeEach
    void setUp() {
        factoryId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .fullName("John Doe Manufacturing")
                .phoneNumber("+233241234567")
                .email("john@factory.com")
                .role(UserRole.FACTORY_OWNER)
                .region("Greater Accra")
                .town("Tema")
                .profileImageUrl("https://example.com/profile.jpg")
                .coverImageUrl("https://example.com/cover.jpg")
                .ratingAvg(4.5)
                .totalOrders(120)
                .isVerified(true)
                .isActive(true)
                .passwordHash("hashed")
                .build();

        testFactory = Factory.builder()
                .id(factoryId)
                .user(testUser)
                .companyName("JD Manufacturing Ltd")
                .description("Premium textile manufacturing in Tema Industrial Area")
                .sectorTags(List.of("textiles", "garments"))
                .machineryList("{\"machines\":[\"Industrial Sewing\",\"Embroidery\"]}")
                .minOrderQuantity(100)
                .maxOrderQuantity(10000)
                .address("Plot 42, Heavy Industrial Area, Tema")
                .verificationStatus(VerificationStatus.VERIFIED)
                .isFeatured(true)
                .responseTimeHours(2.5)
                .completionRate(0.95)
                .build();

        expectedResponse = FactoryResponse.FactoryPublicProfileResponse.builder()
                .factoryId(factoryId)
                .companyName("JD Manufacturing Ltd")
                .description("Premium textile manufacturing in Tema Industrial Area")
                .sectorTags(List.of("textiles", "garments"))
                .ownerId(userId)
                .ownerName("John Doe Manufacturing")
                .ownerPhoneNumber("+233241234567")
                .ownerEmail("john@factory.com")
                .ownerRegion("Greater Accra")
                .ownerTown("Tema")
                .profileImageUrl("https://example.com/profile.jpg")
                .coverImageUrl("https://example.com/cover.jpg")
                .ratingAvg(4.5)
                .totalOrders(120)
                .build();
    }

    @Test
    void getFactoryProfile_returnsFullProfile() {
        when(factoryRepository.findByIdAndDeletedAtIsNull(factoryId)).thenReturn(Optional.of(testFactory));
        when(mapper.toFactoryPublicProfile(testFactory)).thenReturn(expectedResponse);

        FactoryResponse.FactoryPublicProfileResponse result = factoryService.getFactoryProfile(factoryId);

        assertThat(result.getFactoryId()).isEqualTo(factoryId);
        assertThat(result.getCompanyName()).isEqualTo("JD Manufacturing Ltd");
        assertThat(result.getOwnerId()).isEqualTo(userId);
        assertThat(result.getOwnerName()).isEqualTo("John Doe Manufacturing");
        assertThat(result.getRatingAvg()).isEqualTo(4.5);
        assertThat(result.getTotalOrders()).isEqualTo(120);
        assertThat(result.getProfileImageUrl()).isEqualTo("https://example.com/profile.jpg");
        assertThat(result.getCoverImageUrl()).isEqualTo("https://example.com/cover.jpg");

        verify(factoryRepository).findByIdAndDeletedAtIsNull(factoryId);
        verify(mapper).toFactoryPublicProfile(testFactory);
    }

    @Test
    void getFactoryProfile_throwsNotFound_whenFactoryDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(factoryRepository.findByIdAndDeletedAtIsNull(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factoryService.getFactoryProfile(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getFactoryProfileByUserId_returnsFullProfile() {
        when(factoryRepository.findByUserIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(testFactory));
        when(mapper.toFactoryPublicProfile(testFactory)).thenReturn(expectedResponse);

        FactoryResponse.FactoryPublicProfileResponse result = factoryService.getFactoryProfileByUserId(userId);

        assertThat(result.getFactoryId()).isEqualTo(factoryId);
        assertThat(result.getOwnerId()).isEqualTo(userId);
        assertThat(result.getOwnerRegion()).isEqualTo("Greater Accra");
        assertThat(result.getOwnerTown()).isEqualTo("Tema");

        verify(factoryRepository).findByUserIdAndDeletedAtIsNull(userId);
    }

    @Test
    void getFactoryProfileByUserId_throwsNotFound_whenNoFactory() {
        UUID unknownUserId = UUID.randomUUID();
        when(factoryRepository.findByUserIdAndDeletedAtIsNull(unknownUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> factoryService.getFactoryProfileByUserId(unknownUserId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
