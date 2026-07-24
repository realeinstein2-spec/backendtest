package com.makershub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.makershub.dto.request.PaymentRequest;
import java.util.Optional;
import com.makershub.entity.EscrowTransaction;
import com.makershub.entity.Order;
import com.makershub.entity.PaymentTransaction;
import com.makershub.entity.User;
import com.makershub.enums.*;
import com.makershub.exception.BusinessException;
import com.makershub.exception.ResourceNotFoundException;
import com.makershub.exception.UnauthorizedException;
import com.makershub.audit.AuditLogger;
import com.makershub.repository.EscrowTransactionRepository;
import com.makershub.repository.OrderRepository;
import com.makershub.repository.PaymentTransactionRepository;
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
    private final PaymentTransactionRepository paymentTransactionRepository;
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

        if (paystackSecret == null || paystackSecret.isBlank()) {
            throw new BusinessException("Paystack secret key configuration is missing", HttpStatus.INTERNAL_SERVER_ERROR, "MISSING_PAYSTACK_SECRET");
        }

        // Reuse duplicate pending transaction if valid authorizationUrl is present
        Optional<PaymentTransaction> existingOpt = paymentTransactionRepository.findByOrderIdAndStatus(orderId, "PENDING");
        if (existingOpt.isPresent()) {
            PaymentTransaction tx = existingOpt.get();
            return Map.of("reference", tx.getReference(), "authorization_url", tx.getAuthorizationUrl());
        }

        String reference = "mh_" + UUID.randomUUID().toString().replace("-", "").toLowerCase();
        long amountPesewas = order.getAgreedAmountGhs().multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecret);
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "email", sme.getEmail() != null ? sme.getEmail() : (sme.getPhoneNumber() + "@makershub.gh"),
                "amount", amountPesewas,
                "currency", "GHS",
                "reference", reference,
                "callback_url", paystackCallbackUrl,
                "metadata", Map.of("orderId", orderId.toString(), "customerId", sme.getId().toString())
        );
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.postForEntity(paystackBaseUrl + "/transaction/initialize", entity, JsonNode.class);
        } catch (Exception ex) {
            throw new BusinessException("Failed to initialize payment with Paystack: " + ex.getMessage(),
                    HttpStatus.BAD_GATEWAY, "PAYSTACK_INIT_FAILED");
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BusinessException("Paystack returned failure status", HttpStatus.BAD_GATEWAY, "PAYSTACK_INIT_FAILED");
        }

        JsonNode respBody = response.getBody();
        if (!respBody.path("status").asBoolean()) {
            throw new BusinessException("Paystack initialization failed: " + respBody.path("message").asText(),
                    HttpStatus.BAD_GATEWAY, "PAYSTACK_INIT_FAILED");
        }

        String authUrl = respBody.path("data").path("authorization_url").asText("");
        if (!authUrl.startsWith("https://checkout.paystack.com/")) {
            throw new BusinessException("Paystack returned an invalid authorization URL", HttpStatus.BAD_GATEWAY, "INVALID_AUTH_URL");
        }

        PaymentTransaction transaction = PaymentTransaction.builder()
                .reference(reference)
                .orderId(orderId)
                .customerId(sme.getId())
                .amountPesewas(amountPesewas)
                .currency("GHS")
                .status("PENDING")
                .authorizationUrl(authUrl)
                .build();
        paymentTransactionRepository.save(transaction);

        return Map.of("reference", reference, "authorization_url", authUrl);
    }

    @Transactional
    public boolean verifyPayment(String reference) {
        User user = getAuthenticatedUser();
        PaymentTransaction tx = paymentTransactionRepository.findByReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", reference));

        if (!tx.getCustomerId().equals(user.getId())) {
            throw new UnauthorizedException("Transaction does not belong to user");
        }

        if ("PAID".equals(tx.getStatus())) {
            return true;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(paystackSecret);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response;
        try {
            response = restTemplate.exchange(paystackBaseUrl + "/transaction/verify/" + reference,
                    HttpMethod.GET, entity, JsonNode.class);
        } catch (Exception ex) {
            log.error("Failed to contact Paystack for transaction verification", ex);
            return false;
        }

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return false;
        }

        JsonNode respBody = response.getBody();
        if (!respBody.path("status").asBoolean()) {
            return false;
        }

        JsonNode data = respBody.path("data");
        if (!"success".equalsIgnoreCase(data.path("status").asText())) {
            return false;
        }
        if (!reference.equals(data.path("reference").asText())) {
            return false;
        }
        if (tx.getAmountPesewas() != data.path("amount").asLong()) {
            return false;
        }
        if (!"GHS".equalsIgnoreCase(data.path("currency").asText())) {
            return false;
        }

        markChargeSuccessful(tx, data);
        return true;
    }

    @Transactional
    public void markChargeSuccessful(PaymentTransaction transaction, JsonNode paystackData) {
        PaymentTransaction locked = paymentTransactionRepository.findByReferenceForUpdate(transaction.getReference())
                .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", transaction.getReference()));

        if ("PAID".equals(locked.getStatus())) {
            return;
        }

        locked.setStatus("PAID");
        locked.setPaystackTransactionId(paystackData.path("id").asText(""));
        locked.setPaidAt(Instant.now());
        paymentTransactionRepository.save(locked);

        Order order = orderRepository.findById(locked.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", locked.getOrderId().toString()));

        // BUG-02 fix: if the order was already cancelled (or in an unexpected state), do not overwrite it.
        // This guards against race conditions where a webhook arrives after an SME cancels the order.
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            log.error("Webhook received for order {} but current status is {} (expected PAYMENT_PENDING). Skipping escrow creation.",
                    order.getId(), order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.IN_ESCROW);
        orderRepository.save(order);

        // Keep escrow record generation idempotent
        if (escrowRepository.findByOrderId(order.getId()).isEmpty()) {
            BigDecimal amountGhs = BigDecimal.valueOf(locked.getAmountPesewas()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal platformFee = amountGhs.multiply(platformFeePercent).setScale(2, RoundingMode.HALF_UP);
            BigDecimal factoryPayout = amountGhs.subtract(platformFee);

            String channel = paystackData.path("channel").asText();
            PaymentMethod method = null;
            if ("card".equalsIgnoreCase(channel)) {
                method = PaymentMethod.CARD;
            } else if ("mobile_money".equalsIgnoreCase(channel)) {
                String brand = paystackData.path("authorization").path("brand").asText("").toLowerCase();
                if (brand.contains("mtn")) {
                    method = PaymentMethod.MTN_MOMO;
                } else if (brand.contains("vodafone") || brand.contains("telecel")) {
                    method = PaymentMethod.TELECEL;
                } else if (brand.contains("airtel") || brand.contains("tigo")) {
                    method = PaymentMethod.AIRTELTIGO;
                } else {
                    method = PaymentMethod.MTN_MOMO;
                }
            } else if ("bank_transfer".equalsIgnoreCase(channel) || "bank".equalsIgnoreCase(channel)) {
                method = PaymentMethod.BANK_TRANSFER;
            }

            EscrowTransaction escrow = EscrowTransaction.builder()
                    .order(order)
                    .paystackReference(locked.getReference())
                    .amountGhs(amountGhs)
                    .feeAmountGhs(platformFee)
                    .factoryPayoutGhs(factoryPayout)
                    .paymentMethod(method)
                    .escrowStatus(EscrowStatus.HELD)
                    .paidAt(locked.getPaidAt())
                    .build();
            escrowRepository.save(escrow);
            auditLogger.log(AuditAction.UPDATE, "ESCROW", escrow.getId(), EscrowStatus.PENDING.name(), EscrowStatus.HELD.name());
        }
    }

    @Transactional
    public void handleWebhook(HttpServletRequest request, String signature, String payload) {
        if (!signatureVerifier.verify(payload, signature, paystackSecret)) {
            throw new BusinessException("Invalid webhook signature", HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE");
        }
        try {
            JsonNode event = objectMapper.readTree(payload);
            String eventType = event.path("event").asText();
            JsonNode data = event.path("data");
            String reference = data.path("reference").asText();

            if ("charge.success".equals(eventType)) {
                PaymentTransaction tx = paymentTransactionRepository.findByReferenceForUpdate(reference)
                        .orElseThrow(() -> new ResourceNotFoundException("PaymentTransaction", reference));

                if (tx.getAmountPesewas() != data.path("amount").asLong()) {
                    throw new BusinessException("Webhook amount mismatch", HttpStatus.BAD_REQUEST, "PAYMENT_AMOUNT_MISMATCH");
                }
                if (!"GHS".equalsIgnoreCase(data.path("currency").asText())) {
                    throw new BusinessException("Webhook currency mismatch", HttpStatus.BAD_REQUEST, "PAYMENT_CURRENCY_MISMATCH");
                }

                markChargeSuccessful(tx, data);
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Webhook processing failed", HttpStatus.BAD_REQUEST, "WEBHOOK_ERROR");
        }
    }

    private User getAuthenticatedUser() {
        UserDetailsImpl principal = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId().toString()));
    }
}
