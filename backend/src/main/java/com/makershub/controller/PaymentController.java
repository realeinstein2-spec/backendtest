package com.makershub.controller;

import com.makershub.dto.request.PaymentRequest;
import com.makershub.service.PaymentService;
import com.makershub.util.PaystackSignatureVerifier;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaystackSignatureVerifier signatureVerifier;

    @PostMapping("/api/v1/payments/initiate")
    @PreAuthorize("hasAnyRole('SME_OWNER', 'ENTERPRISE')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, String>> initiatePayment(@Valid @RequestBody PaymentRequest.InitiatePaymentRequest request) {
        return ResponseEntity.ok(paymentService.initiatePayment(request));
    }

    @PostMapping("/api/v1/payments/verify")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Boolean>> verifyPayment(@RequestBody Map<String, String> request) {
        String reference = request.get("reference");
        boolean verified = paymentService.verifyPayment(reference);
        return ResponseEntity.ok(Map.of("verified", verified));
    }

    @GetMapping("/api/v1/payments/callback")
    public ResponseEntity<String> paymentCallback() {
        String html = "<html><body><h3>Payment completed. You can return to the app.</h3></body></html>";
        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(html);
    }

    @PostMapping("/webhooks/paystack")
    public ResponseEntity<Void> handlePaystackWebhook(HttpServletRequest request,
                                                      @RequestHeader("X-Paystack-Signature") String signature) {
        String payload = signatureVerifier.readBody(request);
        paymentService.handleWebhook(request, signature, payload);
        return ResponseEntity.ok().build();
    }
}
