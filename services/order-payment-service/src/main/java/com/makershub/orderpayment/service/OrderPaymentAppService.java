package com.makershub.orderpayment.service;

import com.makershub.orderpayment.dto.DisputeDto;
import com.makershub.orderpayment.dto.OrderDto;
import com.makershub.orderpayment.dto.PaymentDto;
import com.makershub.orderpayment.entity.Dispute;
import com.makershub.orderpayment.entity.EscrowTransaction;
import com.makershub.orderpayment.entity.Order;
import com.makershub.orderpayment.entity.PaymentTransaction;
import com.makershub.orderpayment.enums.DisputeStatus;
import com.makershub.orderpayment.enums.EscrowStatus;
import com.makershub.orderpayment.enums.OrderStatus;
import com.makershub.orderpayment.enums.PaymentStatus;
import com.makershub.orderpayment.repository.DisputeRepository;
import com.makershub.orderpayment.repository.EscrowTransactionRepository;
import com.makershub.orderpayment.repository.OrderRepository;
import com.makershub.orderpayment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderPaymentAppService {

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final EscrowTransactionRepository escrowTransactionRepository;
    private final DisputeRepository disputeRepository;

    @Transactional
    public OrderDto.OrderDetailResponse createOrder(UUID smeOwnerId, OrderDto.CreateOrderRequest request) {
        Order order = Order.builder()
                .jobId(request.getJobId())
                .bidId(request.getBidId())
                .smeOwnerId(smeOwnerId)
                .factoryOwnerId(request.getFactoryOwnerId())
                .totalAmount(request.getTotalAmount())
                .status(OrderStatus.PENDING_PAYMENT)
                .build();

        Order saved = orderRepository.save(order);
        return toOrderResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderDto.OrderDetailResponse> getOrdersForUser(UUID userId, Pageable pageable) {
        return orderRepository.findBySmeOwnerId(userId, pageable)
                .map(this::toOrderResponse);
    }

    @Transactional(readOnly = true)
    public OrderDto.OrderDetailResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return toOrderResponse(order);
    }

    @Transactional
    public PaymentDto.PaymentInitResponse initializePayment(UUID userId, PaymentDto.InitializePaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getSmeOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        String reference = "PAY-" + UUID.randomUUID().toString().substring(0, 8);

        PaymentTransaction transaction = PaymentTransaction.builder()
                .orderId(order.getId())
                .paymentReference(reference)
                .amount(request.getAmount())
                .status(PaymentStatus.SUCCESSFUL)
                .build();

        paymentTransactionRepository.save(transaction);

        // Move order to IN_PRODUCTION & create HELD escrow
        order.setStatus(OrderStatus.IN_PRODUCTION);
        orderRepository.save(order);

        EscrowTransaction escrow = EscrowTransaction.builder()
                .orderId(order.getId())
                .amount(request.getAmount())
                .status(EscrowStatus.HELD)
                .build();

        escrowTransactionRepository.save(escrow);

        return PaymentDto.PaymentInitResponse.builder()
                .paymentReference(reference)
                .authorizationUrl("https://checkout.paystack.com/" + reference)
                .status(PaymentStatus.SUCCESSFUL)
                .build();
    }

    @Transactional
    public DisputeDto.DisputeDetailResponse raiseDispute(UUID userId, DisputeDto.RaiseDisputeRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        order.setStatus(OrderStatus.DISPUTED);
        orderRepository.save(order);

        Dispute dispute = Dispute.builder()
                .orderId(order.getId())
                .raisedByUserId(userId)
                .reason(request.getReason())
                .status(DisputeStatus.OPEN)
                .build();

        Dispute saved = disputeRepository.save(dispute);
        return toDisputeResponse(saved);
    }

    private OrderDto.OrderDetailResponse toOrderResponse(Order o) {
        return OrderDto.OrderDetailResponse.builder()
                .id(o.getId())
                .jobId(o.getJobId())
                .bidId(o.getBidId())
                .smeOwnerId(o.getSmeOwnerId())
                .factoryOwnerId(o.getFactoryOwnerId())
                .totalAmount(o.getTotalAmount())
                .status(o.getStatus())
                .createdAt(o.getCreatedAt())
                .build();
    }

    private DisputeDto.DisputeDetailResponse toDisputeResponse(Dispute d) {
        return DisputeDto.DisputeDetailResponse.builder()
                .id(d.getId())
                .orderId(d.getOrderId())
                .raisedByUserId(d.getRaisedByUserId())
                .reason(d.getReason())
                .status(d.getStatus())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
