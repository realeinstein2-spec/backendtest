package com.makershub.orderpayment.service;

import com.makershub.orderpayment.dto.OrderDto;
import com.makershub.orderpayment.dto.PaymentDto;
import com.makershub.orderpayment.entity.EscrowTransaction;
import com.makershub.orderpayment.entity.Order;
import com.makershub.orderpayment.entity.PaymentTransaction;
import com.makershub.orderpayment.enums.OrderStatus;
import com.makershub.orderpayment.enums.PaymentStatus;
import com.makershub.orderpayment.repository.DisputeRepository;
import com.makershub.orderpayment.repository.EscrowTransactionRepository;
import com.makershub.orderpayment.repository.OrderRepository;
import com.makershub.orderpayment.repository.PaymentTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPaymentAppServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private EscrowTransactionRepository escrowTransactionRepository;

    @Mock
    private DisputeRepository disputeRepository;

    @InjectMocks
    private OrderPaymentAppService orderPaymentAppService;

    @Test
    void createOrder_success() {
        UUID smeId = UUID.randomUUID();
        UUID factoryId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        UUID bidId = UUID.randomUUID();

        OrderDto.CreateOrderRequest request = new OrderDto.CreateOrderRequest();
        request.setJobId(jobId);
        request.setBidId(bidId);
        request.setFactoryOwnerId(factoryId);
        request.setTotalAmount(BigDecimal.valueOf(10000));

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> {
            Order o = i.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        OrderDto.OrderDetailResponse response = orderPaymentAppService.createOrder(smeId, request);

        assertThat(response).isNotNull();
        assertThat(response.getSmeOwnerId()).isEqualTo(smeId);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
    }

    @Test
    void initializePayment_success() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        Order order = Order.builder()
                .id(orderId)
                .smeOwnerId(userId)
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        PaymentDto.InitializePaymentRequest request = new PaymentDto.InitializePaymentRequest();
        request.setOrderId(orderId);
        request.setAmount(BigDecimal.valueOf(10000));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaymentDto.PaymentInitResponse response = orderPaymentAppService.initializePayment(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESSFUL);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
        verify(paymentTransactionRepository).save(any(PaymentTransaction.class));
        verify(escrowTransactionRepository).save(any(EscrowTransaction.class));
    }
}
