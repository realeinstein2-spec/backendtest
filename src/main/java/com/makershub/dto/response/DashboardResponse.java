package com.makershub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

public final class DashboardResponse {

    private DashboardResponse() {}

    @Data
    @Builder
    public static class DashboardStatsResponse {
        private long totalUsers;
        private long verifiedFactories;
        private long pendingVerifications;
        private long totalOrders;
        private long completedOrders;
        private long openDisputes;
        private BigDecimal gmvLast30Days;
        private BigDecimal feeRevenueLast30Days;
        private double escrowSuccessRate;
    }
}
