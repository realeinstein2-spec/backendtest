package com.makershub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.makershub.dto.request.PaymentRequest;
import com.makershub.entity.EscrowTransaction;
import com.makershub.entity.Order;
import com.makershub.entity.User;
import com.makershub.enums.*;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.exception.UnauthorizedException;
import com.makershub.audit.AuditLogger;
import com.makershub.repository.EscrowTransactionRepository;
import com.makershub.repository.OrderRepository;
import com.makershub.repository.UserRepository;
import com.makershub.security.UserDetailsImpl;
import com.makershub.util.PaystackSignatureVerifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final EscrowTransactionRepository escrowRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;
    private final PaystackSignatureVerifier signatureVerifier;
    private final RestTemplate restTemplate;
    // M-8: Use Spring-managed ObjectMapper bean instead of instantiating per-request
    private final ObjectMapper objectMapper;

    @Value("${makershub.platform.fee-percent:0.035}")
    private BigDecimal platformFeePercent;

    @Value("${makershub.paystack.secret-key:}")
    private String paystackSecret;

    @Value("${makershub.paystack.base-url:https://api.paystack.co}")
    private String paystackBaseUrl;

    @Value("${makershub.paystack.callback-url:https://makershub.gh/payment/callback}")
    private String paystackCallbackUrl;

    @Transactional
    public Map<String, String> initiatePayment(PaymentRequest.InitiatePaymentRequest request) {
        User sme = getAuthenticatedUser();
        UUID orderId = UUID.fromString(request.getOrderId());
        Order order = orderRepository.findByIdAndDeletedAtIsNull(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", request.getOrderId()));
        if (!order.getSme().getId().equals(sme.getId())) {
            throw new UnauthorizedException("You can only pay for your own orders");
        }
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException("Order is not awaiting payment", HttpStatus.CONFLICT, "ORDER_NOT_AWAITING_PAYMENT");
        }

        // C-14: Prevent duplicate escrow records if initiatePayment is called more than once
        if (escrowRepository.findByOrderId(orderId).isPresent()) {
            log.warn("Payment already initiated for order {}. Returning existing reference.", orderId);
            EscrowTransaction existing = escrowRepository.findByOrderId(orderId).get();
            return Map.of("reference", existing.getPaystackReference(),
                    "authorization_url", "https://paystack.com/pay/" + existing.getPaystackReference());
        }

        String reference = "MKH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        BigDecimal amountKobo = order.getAgreedAmountGhs().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP);

        EscrowTransaction escrow = EscrowTransaction.builder()
                .order(order)
                .paystackReference(reference)
                .amountGhs(order.getAgreedAmountGhs())
                .feeAmountGhs(order.getPlatformFeeGhs())
                .factoryPayoutGhs(order.getFactoryPayoutGhs())
                .paymentMethod(request.getPaymentMethod())
                .escrowStatus(EscrowStatus.PENDING)
                .build();
        escrowRepository.save(escrow);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "email", sme.getEmail() != null ? sme.getEmail() : sme.getPhoneNumber() + "@makershub.gh",
                "amount", amountKobo,
                "reference", reference,
                "callback_url", paystackCallbackUrl,
                "metadata", Map.of("order_id", orderId.toString(), "platform", "mobile")
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        if (paystackSecret.isBlank()) {
            log.warn("Paystack secret not configured; returning simulated reference.");
            return Map.of("reference", reference, "authorization_url", "https://paystack.com/pay/" + reference);
        }
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                paystackBaseUrl + "/transaction/initialize", entity, JsonNode.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException("Failed to initialize payment", HttpStatus.BAD_GATEWAY, "PAYSTACK_INIT_FAILED");
        }
        String authUrl = response.getBody().path("data").path("authorization_url").asText();
        return Map.of("reference", reference, "authorization_url", authUrl);
    }

    @Transactional
    public void handleWebhook(HttpServletRequest request, String signature, String payload) {
        if (!signatureVerifier.verify(payload, signature, paystackSecret)) {
            throw new BusinessException("Invalid webhook signature", HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE");
        }
        try {
            // M-8: Use injected ObjectMapper bean
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.path("event").asText();
            JsonNode data = event.path("data");
            String reference = data.path("reference").asText();
            if ("charge.success".equals(eventType)) {
                processSuccessfulCharge(reference, data);
            } else if ("charge.failed".equals(eventType)) {
                // M-10: Handle failed charges by marking escrow as pending-failed state
                handleFailedCharge(reference);
            } else if ("transfer.success".equals(eventType)) {
                // M-9: Log transfer success and confirm payout audit trail
                log.info("Paystack transfer.success received for reference: {}", reference);
                escrowRepository.findByPaystackReference(reference).ifPresent(escrow -> {
                    auditLogger.log(AuditAction.ESCROW_RELEASE, "ESCROW", escrow.getId(),
                            escrow.getEscrowStatus().name(), "PAYOUT_CONFIRMED");
                });
            }
        } catch (Exception ex) {
            throw new BusinessException("Webhook processing failed", HttpStatus.BAD_REQUEST, "WEBHOOK_ERROR");
        }
    }

    private void processSuccessfulCharge(String reference, JsonNode data) {
        EscrowTransaction escrow = escrowRepository.findByPaystackReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("EscrowTransaction", reference));

        // Idempotency check
        if (escrow.getEscrowStatus() == EscrowStatus.HELD || escrow.getEscrowStatus() == EscrowStatus.RELEASED) {
            log.info("Paystack webhook reference {} already processed. Skipping.", reference);
            return;
        }

        // Amount verification check
        long actualAmountSubunits = data.path("amount").asLong();
        long expectedAmountSubunits = escrow.getAmountGhs()
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        if (actualAmountSubunits != expectedAmountSubunits) {
            log.error("Paystack webhook amount mismatch for reference {}. Expected {} subunits, got {}",
                    reference, expectedAmountSubunits, actualAmountSubunits);
            throw new BusinessException("Payment amount mismatch", HttpStatus.BAD_REQUEST, "PAYMENT_AMOUNT_MISMATCH");
        }

        escrow.setEscrowStatus(EscrowStatus.HELD);
        escrow.setPaidAt(Instant.now());
        String authCode = data.path("authorization").path("authorization_code").asText(null);
        escrow.setPaystackAuthorizationCode(authCode);
        escrowRepository.save(escrow);

        Order order = escrow.getOrder();
        order.setStatus(OrderStatus.IN_ESCROW);
        orderRepository.save(order);
        // Fix: Use UPDATE action for a status change; ESCROW_RELEASE is reserved for actual release events
        auditLogger.log(AuditAction.UPDATE, "ESCROW", escrow.getId(), EscrowStatus.PENDING.name(), EscrowStatus.HELD.name());
    }

    // M-10: Handle charge.failed — log warning and keep escrow in PENDING state (no FAILED status in enum)
    private void handleFailedCharge(String reference) {
        escrowRepository.findByPaystackReference(reference).ifPresent(escrow -> {
            if (escrow.getEscrowStatus() == EscrowStatus.PENDING) {
                // EscrowStatus.FAILED does not exist; log the failure for operator visibility
                log.warn("Payment failed for reference {}. Escrow {} remains in PENDING state.", reference, escrow.getId());
                auditLogger.log(AuditAction.ESCROW_RELEASE, "ESCROW", escrow.getId(),
                        EscrowStatus.PENDING.name(), "CHARGE_FAILED");
            }
        });
    }

    private User getAuthenticatedUser() {
        UserDetailsImpl principal = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId().toString()));
    }
}
