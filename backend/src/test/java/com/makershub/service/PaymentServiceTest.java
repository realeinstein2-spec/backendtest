package com.makershub.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.makershub.dto.request.PaymentRequest;
import com.makershub.entity.EscrowTransaction;
import com.makershub.entity.Order;
import com.makershub.entity.PaymentTransaction;
import com.makershub.entity.User;
import com.makershub.enums.EscrowStatus;
import com.makershub.enums.OrderStatus;
import com.makershub.enums.UserRole;
import com.makershub.exception.BusinessException;
import com.makershub.exception.UnauthorizedException;
import com.makershub.audit.AuditLogger;
import com.makershub.repository.EscrowTransactionRepository;
import com.makershub.repository.OrderRepository;
import com.makershub.repository.PaymentTransactionRepository;
import com.makershub.repository.UserRepository;
import com.makershub.security.UserDetailsImpl;
import com.makershub.util.PaystackSignatureVerifier;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private EscrowTransactionRepository escrowRepository;
    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private PaystackSignatureVerifier signatureVerifier;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PaymentService paymentService;

    private User testUser;
    private Order testOrder;
    private PaymentRequest.InitiatePaymentRequest initRequest;
    private ObjectMapper realMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .phoneNumber("+233240000000")
                .fullName("Test Buyer")
                .role(UserRole.SME_OWNER)
                .build();

        testOrder = Order.builder()
                .id(UUID.randomUUID())
                .sme(testUser)
                .agreedAmountGhs(new BigDecimal("100.00"))
                .platformFeeGhs(new BigDecimal("3.50"))
                .factoryPayoutGhs(new BigDecimal("96.50"))
                .status(OrderStatus.PAYMENT_PENDING)
                .build();

        initRequest = new PaymentRequest.InitiatePaymentRequest();
        initRequest.setOrderId(testOrder.getId().toString());

        // Setup security context mock
        UserDetailsImpl principal = new UserDetailsImpl(testUser);
        Authentication auth = mock(Authentication.class);
        lenient().when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext context = mock(SecurityContext.class);
        lenient().when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        lenient().when(userRepository.findByIdAndDeletedAtIsNull(testUser.getId())).thenReturn(Optional.of(testUser));
        lenient().when(orderRepository.findByIdAndDeletedAtIsNull(testOrder.getId())).thenReturn(Optional.of(testOrder));

        ReflectionTestUtils.setField(paymentService, "paystackSecret", "sk_test_mocksecret");
        ReflectionTestUtils.setField(paymentService, "paystackBaseUrl", "https://api.paystack.co");
        ReflectionTestUtils.setField(paymentService, "paystackCallbackUrl", "https://callback.url");
        ReflectionTestUtils.setField(paymentService, "platformFeePercent", new BigDecimal("0.035"));
    }

    // 1. Missing secret key rejects initiation
    @Test
    void initiate_MissingSecret_ThrowsException() {
        ReflectionTestUtils.setField(paymentService, "paystackSecret", "");
        BusinessException ex = assertThrows(BusinessException.class, () -> paymentService.initiatePayment(initRequest));
        assertEquals("MISSING_PAYSTACK_SECRET", ex.getErrorCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
    }

    // 2. Paystack initialization returns only valid checkout.paystack.com URLs
    @Test
    void initiate_InvalidUrl_ThrowsException() {
        String invalidResponse = "{\"status\":true,\"data\":{\"authorization_url\":\"https://invalid.url/checkout\"}}";
        JsonNode node = null;
        try {
            node = realMapper.readTree(invalidResponse);
        } catch (Exception e) {}
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.ok(node));

        BusinessException ex = assertThrows(BusinessException.class, () -> paymentService.initiatePayment(initRequest));
        assertEquals("INVALID_AUTH_URL", ex.getErrorCode());
    }

    // 3. Invalid Paystack response rejects initiation
    @Test
    void initiate_PaystackFailureStatus_ThrowsException() {
        String failResponse = "{\"status\":false,\"message\":\"Invalid amount\"}";
        JsonNode node = null;
        try {
            node = realMapper.readTree(failResponse);
        } catch (Exception e) {}
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), any())).thenReturn(ResponseEntity.ok(node));

        BusinessException ex = assertThrows(BusinessException.class, () -> paymentService.initiatePayment(initRequest));
        assertEquals("PAYSTACK_INIT_FAILED", ex.getErrorCode());
    }

    // 4. Duplicate pending initiation reuses stored checkout URL
    @Test
    void initiate_DuplicatePending_ReusesUrl() {
        PaymentTransaction pendingTx = PaymentTransaction.builder()
                .reference("mh_duplicate_ref")
                .orderId(testOrder.getId())
                .customerId(testUser.getId())
                .status("PENDING")
                .authorizationUrl("https://checkout.paystack.com/storedtoken")
                .build();
        when(paymentTransactionRepository.findByOrderIdAndStatus(testOrder.getId(), "PENDING")).thenReturn(Optional.of(pendingTx));

        Map<String, String> resp = paymentService.initiatePayment(initRequest);
        assertEquals("mh_duplicate_ref", resp.get("reference"));
        assertEquals("https://checkout.paystack.com/storedtoken", resp.get("authorization_url"));
        verify(restTemplate, never()).postForEntity(anyString(), any(HttpEntity.class), any());
    }

    // 5. Verify rejects another user's reference
    @Test
    void verify_ForeignUser_ThrowsException() {
        PaymentTransaction foreignTx = PaymentTransaction.builder()
                .reference("mh_foreign")
                .customerId(UUID.randomUUID()) // different user
                .build();
        when(paymentTransactionRepository.findByReference("mh_foreign")).thenReturn(Optional.of(foreignTx));

        assertThrows(UnauthorizedException.class, () -> paymentService.verifyPayment("mh_foreign"));
    }

    // 6. Verify rejects wrong amount/currency/reference and non-success status
    @Test
    void verify_MismatchedDetails_ReturnsFalse() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .reference("mh_testref")
                .customerId(testUser.getId())
                .amountPesewas(10000L)
                .currency("GHS")
                .status("PENDING")
                .build();
        when(paymentTransactionRepository.findByReference("mh_testref")).thenReturn(Optional.of(tx));

        // Mock mismatched amount
        String verifyResp = "{\"status\":true,\"data\":{\"status\":\"success\",\"reference\":\"mh_testref\",\"amount\":9000,\"currency\":\"GHS\"}}";
        JsonNode node = null;
        try {
            node = realMapper.readTree(verifyResp);
        } catch (Exception e) {}
        when(restTemplate.exchange(anyString(), any(), any(HttpEntity.class), eq(JsonNode.class))).thenReturn(ResponseEntity.ok(node));

        assertFalse(paymentService.verifyPayment("mh_testref"));
    }

    // 7. Verify success updates order/escrow once
    @Test
    void verify_Success_UpdatesState() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .reference("mh_testref")
                .orderId(testOrder.getId())
                .customerId(testUser.getId())
                .amountPesewas(10000L)
                .currency("GHS")
                .status("PENDING")
                .build();
        when(paymentTransactionRepository.findByReference("mh_testref")).thenReturn(Optional.of(tx));
        when(paymentTransactionRepository.findByReferenceForUpdate("mh_testref")).thenReturn(Optional.of(tx));
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(escrowRepository.findByOrderId(testOrder.getId())).thenReturn(Optional.empty());

        String verifyResp = "{\"status\":true,\"data\":{\"id\":\"12345\",\"status\":\"success\",\"reference\":\"mh_testref\",\"amount\":10000,\"currency\":\"GHS\",\"channel\":\"card\"}}";
        JsonNode node = null;
        try {
            node = realMapper.readTree(verifyResp);
        } catch (Exception e) {}
        when(restTemplate.exchange(anyString(), any(), any(HttpEntity.class), eq(JsonNode.class))).thenReturn(ResponseEntity.ok(node));

        assertTrue(paymentService.verifyPayment("mh_testref"));
        assertEquals("PAID", tx.getStatus());
        assertEquals(OrderStatus.IN_ESCROW, testOrder.getStatus());
        verify(escrowRepository, times(1)).save(any(EscrowTransaction.class));
    }

    // 8. Valid signed charge.success webhook updates once
    @Test
    void webhook_ValidSignatureSuccess_UpdatesState() throws Exception {
        PaymentTransaction tx = PaymentTransaction.builder()
                .reference("mh_testref")
                .orderId(testOrder.getId())
                .customerId(testUser.getId())
                .amountPesewas(10000L)
                .currency("GHS")
                .status("PENDING")
                .build();
        when(signatureVerifier.verify(anyString(), anyString(), anyString())).thenReturn(true);
        when(paymentTransactionRepository.findByReferenceForUpdate("mh_testref")).thenReturn(Optional.of(tx));
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));
        when(escrowRepository.findByOrderId(testOrder.getId())).thenReturn(Optional.empty());

        String payload = "{\"event\":\"charge.success\",\"data\":{\"id\":\"123\",\"reference\":\"mh_testref\",\"amount\":10000,\"currency\":\"GHS\",\"channel\":\"card\"}}";
        JsonNode node = realMapper.readTree(payload);
        when(objectMapper.readTree(payload)).thenReturn(node);

        paymentService.handleWebhook(mock(HttpServletRequest.class), "sig", payload);
        assertEquals("PAID", tx.getStatus());
        assertEquals(OrderStatus.IN_ESCROW, testOrder.getStatus());
        verify(escrowRepository, times(1)).save(any(EscrowTransaction.class));
    }

    // 9. Invalid webhook signature is rejected
    @Test
    void webhook_InvalidSignature_ThrowsException() {
        when(signatureVerifier.verify(anyString(), anyString(), anyString())).thenReturn(false);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                paymentService.handleWebhook(mock(HttpServletRequest.class), "invalid_sig", "payload"));
        assertEquals("INVALID_SIGNATURE", ex.getErrorCode());
    }

    // 10. Calling webhook then verify, or verify then webhook, does not duplicate escrow changes
    @Test
    void webhookVerify_Concurrently_Idempotent() throws Exception {
        PaymentTransaction tx = PaymentTransaction.builder()
                .reference("mh_testref")
                .orderId(testOrder.getId())
                .customerId(testUser.getId())
                .amountPesewas(10000L)
                .currency("GHS")
                .status("PAID") // already PAID!
                .build();
        when(paymentTransactionRepository.findByReferenceForUpdate("mh_testref")).thenReturn(Optional.of(tx));

        // Call markChargeSuccessful directly simulating a second thread
        String payload = "{\"id\":\"123\",\"reference\":\"mh_testref\",\"amount\":10000,\"currency\":\"GHS\",\"channel\":\"card\"}";
        JsonNode dataNode = realMapper.readTree(payload);

        paymentService.markChargeSuccessful(tx, dataNode);

        // Verify that order status save and escrow transaction save are NOT triggered again
        verify(orderRepository, never()).save(any(Order.class));
        verify(escrowRepository, never()).save(any(EscrowTransaction.class));
    }

    // 11. Webhook for cancelled order skips escrow update (BUG-02 fix)
    @Test
    void markChargeSuccessful_skipsEscrow_whenOrderCancelled() throws Exception {
        testOrder.setStatus(OrderStatus.CANCELLED);
        PaymentTransaction tx = PaymentTransaction.builder()
                .reference("mh_cancelled_ref")
                .orderId(testOrder.getId())
                .customerId(testUser.getId())
                .amountPesewas(10000L)
                .currency("GHS")
                .status("PENDING")
                .build();
        when(paymentTransactionRepository.findByReferenceForUpdate("mh_cancelled_ref")).thenReturn(Optional.of(tx));
        when(orderRepository.findById(testOrder.getId())).thenReturn(Optional.of(testOrder));

        String payload = "{\"id\":\"123\",\"reference\":\"mh_cancelled_ref\",\"amount\":10000,\"currency\":\"GHS\",\"channel\":\"card\"}";
        JsonNode dataNode = realMapper.readTree(payload);

        paymentService.markChargeSuccessful(tx, dataNode);

        // Verify status remains CANCELLED and no escrow record created
        assertEquals(OrderStatus.CANCELLED, testOrder.getStatus());
        verify(escrowRepository, never()).save(any(EscrowTransaction.class));
    }
}
