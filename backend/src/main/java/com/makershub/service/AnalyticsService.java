package com.makershub.service;

import com.makershub.dto.response.DashboardResponse;
import com.makershub.enums.DisputeStatus;
import com.makershub.enums.OrderStatus;
import com.makershub.enums.UserRole;
import com.makershub.enums.VerificationStatus;
import com.makershub.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    // M-12: Configurable fee rate instead of hardcoded constant
    @Value("${makershub.platform.fee-percent:0.035}")
    private BigDecimal platformFeePercent;

    private final UserRepository userRepository;
    private final FactoryRepository factoryRepository;
    private final OrderRepository orderRepository;
    private final DisputeRepository disputeRepository;
    private final EscrowTransactionRepository escrowRepository;

    @Transactional(readOnly = true)
    public DashboardResponse.DashboardStatsResponse getDashboardStats() {
        Instant since = Instant.now().minus(30, ChronoUnit.DAYS);
        long totalUsers = userRepository.count();
        long verifiedFactories = factoryRepository.countByVerificationStatus(VerificationStatus.VERIFIED);
        long pendingVerifications = factoryRepository.countByVerificationStatus(VerificationStatus.PENDING);
        long totalOrders = orderRepository.count();
        // M-11: Use `since` instead of Instant.EPOCH so completedOrders reflects the last 30 days
        long completedOrders = orderRepository.countCompletedSince(since);
        long openDisputes = disputeRepository.countByStatus(DisputeStatus.OPEN) + disputeRepository.countByStatus(DisputeStatus.UNDER_REVIEW);
        BigDecimal gmv = orderRepository.sumGmvSince(since);
        BigDecimal feeRevenue = calculateFeeRevenue(gmv);
        double escrowSuccessRate = calculateEscrowSuccessRate();

        return DashboardResponse.DashboardStatsResponse.builder()
                .totalUsers(totalUsers)
                .verifiedFactories(verifiedFactories)
                .pendingVerifications(pendingVerifications)
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .openDisputes(openDisputes)
                .gmvLast30Days(gmv)
                .feeRevenueLast30Days(feeRevenue)
                .escrowSuccessRate(escrowSuccessRate)
                .build();
    }

    private BigDecimal calculateFeeRevenue(BigDecimal gmv) {
        if (gmv == null) return BigDecimal.ZERO;
        return gmv.multiply(platformFeePercent).setScale(2, RoundingMode.HALF_UP);
    }

    private double calculateEscrowSuccessRate() {
        long total = escrowRepository.count();
        long released = escrowRepository.countByEscrowStatus(com.makershub.enums.EscrowStatus.RELEASED);
        if (total == 0) return 100.0;
        return Math.round((released * 10000.0) / total) / 100.0;
    }
}
