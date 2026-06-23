package src.main.java.controller;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;

import model.SponsorProgram;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import repository.UserRepository;
import service.SponsorService;
import src.main.java.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sponsors")
@CrossOrigin(origins = "*")
public class SponsorController {

    private static final Logger log = LoggerFactory.getLogger(SponsorController.class);

    private final UserRepository userRepository;
    private final SponsorService sponsorService;

    public SponsorController(UserRepository userRepository, SponsorService sponsorService) {
        this.userRepository = userRepository;
        this.sponsorService = sponsorService;
    }

    @PostMapping("/coupons")
    public ResponseEntity<?> createCoupon(@RequestBody Map<String, String> body, Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Coupon code is required"));
        }
        String discountStr = body.get("discount");
        if (discountStr == null || discountStr.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Discount percentage is required"));
        }
        int discount;
        try {
            discount = Integer.parseInt(discountStr);
            if (discount < 1 || discount > 100) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Discount must be between 1 and 100"));
        }
        int maxUses = 100;
        String maxUsesStr = body.get("maxUses");
        if (maxUsesStr != null && !maxUsesStr.isBlank()) {
            try { maxUses = Math.max(1, Integer.parseInt(maxUsesStr)); } catch (NumberFormatException ignored) {}
        }
        String expiryDate = body.get("expiryDate");
        model.SponsorCoupon coupon = sponsorService.createCoupon(sponsor, code, discount, expiryDate, maxUses);
        return ResponseEntity.ok(Map.of(
            "id",         coupon.getId(),
            "code",       coupon.getCode(),
            "discount",   coupon.getDiscount(),
            "maxUses",    coupon.getMaxUses(),
            "usedCount",  coupon.getUsedCount(),
            "expiryDate", coupon.getExpiryDate() != null ? coupon.getExpiryDate() : "",
            "active",     coupon.isActive()
        ));
    }

    @GetMapping("/coupons")
    public ResponseEntity<?> listCoupons(Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        List<Map<String, Object>> result = sponsorService.getActiveCoupons(sponsor).stream()
            .map(c -> Map.<String, Object>of(
                "id",         c.getId(),
                "code",       c.getCode() != null ? c.getCode() : "",
                "discount",   c.getDiscount() != null ? c.getDiscount() : 0,
                "maxUses",    c.getMaxUses() != null ? c.getMaxUses() : 0,
                "usedCount",  c.getUsedCount() != null ? c.getUsedCount() : 0,
                "expiryDate", c.getExpiryDate() != null ? c.getExpiryDate() : "",
                "active",     c.isActive()
            ))
            .toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/programs")
    public ResponseEntity<?> createProgram(@RequestBody Map<String, String> body, Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        String title = body.get("title");
        if (title == null || title.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Program title is required"));
        }
        String description = body.get("description");
        String status = body.get("status");
        SponsorProgram program = sponsorService.createProgram(sponsor, title, description, status);
        return ResponseEntity.ok(Map.of(
            "id",            program.getId(),
            "title",         program.getTitle() != null ? program.getTitle() : "",
            "description",   program.getDescription() != null ? program.getDescription() : "",
            "status",        program.getStatus() != null ? program.getStatus() : "ACTIVE",
            "paymentStatus", program.getPaymentStatus() != null ? program.getPaymentStatus() : "PENDING"
        ));
    }

    @GetMapping("/programs")
    public ResponseEntity<?> listPrograms(Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        List<Map<String, Object>> result = sponsorService.getPrograms(sponsor).stream()
            .map(p -> Map.<String, Object>of(
                "id",          p.getId(),
                "title",       p.getTitle() != null ? p.getTitle() : "",
                "description", p.getDescription() != null ? p.getDescription() : "",
                "status",      p.getStatus() != null ? p.getStatus() : "PENDING"
            ))
            .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/me/stats")
    public ResponseEntity<Map<String, Object>> getSponsorStats(Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        Map<String, Object> stats = sponsorService.getMetrics(sponsor);
        stats.put("programs", sponsorService.getPrograms(sponsor));
        stats.put("coupons", sponsorService.getActiveCoupons(sponsor));
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/programs/{id}/checkout")
    public ResponseEntity<Map<String, Object>> createCheckout(
            @PathVariable Long id, Authentication auth) {

        User sponsor = getUserFromAuth(auth);

        SponsorProgram program = sponsorService.findProgramById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Program not found"));

        if (!program.getSponsor().getId().equals(sponsor.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
        }

        if (!"PENDING".equals(program.getPaymentStatus())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Program is not in PENDING payment state"));
        }

        if (program.getFundingAmount() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Program has no funding amount set"));
        }

        try {
            long amountInCents = program.getFundingAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addLineItem(
                        SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("usd")
                                    .setUnitAmount(amountInCents)
                                    .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(program.getTitle())
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .setSuccessUrl("https://localhost:8443/sponsor-dashboard.html?payment=success")
                    .setCancelUrl("https://localhost:8443/sponsor-dashboard.html?payment=cancelled")
                    .putMetadata("programId", program.getId().toString())
                    .build();

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("checkout-program-" + program.getId())
                    .build();

            Session session = Session.create(params, options);
            program.setStripeSessionId(session.getId());
            sponsorService.saveProgram(program);

            return ResponseEntity.ok(Map.of("url", session.getUrl()));

        } catch (StripeException e) {
            log.error("Stripe error creating checkout for program {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Payment service unavailable, please try again later"));
        }
    }

    @GetMapping("/talent")
    public ResponseEntity<?> getTalent(Authentication auth) {
        getUserFromAuth(auth); // verify authenticated sponsor
        List<Map<String, Object>> result = sponsorService.getTalentPool().stream()
            .map(u -> Map.<String, Object>of(
                "id",       u.getId(),
                "name",     u.getName() != null ? u.getName() : "",
                "email",    u.getEmail() != null ? u.getEmail() : "",
                "location", u.getLocation() != null ? u.getLocation() : ""
            ))
            .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/reports/export")
    public ResponseEntity<String> exportReport(Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        List<model.SponsorProgram> programs = sponsorService.getPrograms(sponsor);
        List<model.SponsorCoupon>  coupons  = sponsorService.getActiveCoupons(sponsor);

        StringBuilder csv = new StringBuilder();
        csv.append("Programs\n");
        csv.append("Title,Description,Status,Payment Status\n");
        for (model.SponsorProgram p : programs) {
            csv.append(csvEscape(p.getTitle())).append(',')
               .append(csvEscape(p.getDescription())).append(',')
               .append(csvEscape(p.getStatus())).append(',')
               .append(csvEscape(p.getPaymentStatus())).append('\n');
        }
        csv.append("\nCoupons\n");
        csv.append("Code,Discount (%),Max Uses,Used Count,Expiry Date,Active\n");
        for (model.SponsorCoupon c : coupons) {
            csv.append(csvEscape(c.getCode())).append(',')
               .append(c.getDiscount() != null ? c.getDiscount() : 0).append(',')
               .append(c.getMaxUses()   != null ? c.getMaxUses()   : 0).append(',')
               .append(c.getUsedCount() != null ? c.getUsedCount() : 0).append(',')
               .append(csvEscape(c.getExpiryDate())).append(',')
               .append(c.isActive()).append('\n');
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sponsor-report.csv\"");
        return new ResponseEntity<>(csv.toString(), headers, HttpStatus.OK);
    }

    private static String csvEscape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private User getUserFromAuth(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
