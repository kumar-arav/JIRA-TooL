package com.flowsync.service.impl;

import com.flowsync.dto.request.AIGenerateRequest;
import com.flowsync.dto.request.AcceptAITasksRequest;
import com.flowsync.dto.response.AITaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowsync.entity.Sprint;
import com.flowsync.entity.Ticket;
import com.flowsync.enums.Priority;
import com.flowsync.enums.TicketStatus;
import com.flowsync.repository.SprintRepository;
import com.flowsync.repository.TicketRepository;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIServiceImpl {

    private final SprintRepository sprintRepository;
    private final TicketRepository ticketRepository;

    @Value("${app.ai.mock:true}")
    private boolean mockMode;

    @Value("${app.ai.groq-key:}")
    private String groqApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AITaskResponse generateTasks(AIGenerateRequest req) {
        log.info("AI task generation for: {}", req.getProjectDescription());

        String projectDesc = req.getProjectDescription();
        List<AITaskResponse.AITask> tasks = new ArrayList<>();

        try {
            if (groqApiKey == null || groqApiKey.trim().isEmpty() || "your-groq-key-here".equals(groqApiKey.trim())) {
                log.warn("Groq API Key is not configured. Live API generation will be bypassed.");
            } else {
                String url = "https://api.groq.com/openai/v1/chat/completions";
                String apiKey = groqApiKey.trim();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.1-8b-instant");

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", 
                "You are an expert sprint planner. You must generate a list of software tasks based on the project description. " +
                "You MUST respond ONLY with a valid JSON array, containing objects with keys: 'title', 'description', 'storyPoints', 'priority', 'suggestedRole', 'type'. " +
                "Requirements:\n" +
                "- priority must be: CRITICAL, HIGH, MEDIUM, or LOW.\n" +
                "- suggestedRole must be: Developer or Tester.\n" +
                "- storyPoints must be an integer (e.g. 1, 2, 3, 5, 8, 13).\n" +
                "- Output ONLY raw JSON. No markdown backticks, no code formatting, no explanation."
            ));
            messages.add(Map.of("role", "user", "content", projectDesc != null ? projectDesc : "Hospital Management"));

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.2);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices").path(0).path("message").path("content").asText().trim();
                
                // Clean markdown code blocks if any
                if (content.startsWith("```")) {
                    content = content.replaceAll("^```json\\s*", "").replaceAll("^```\\s*", "").replaceAll("\\s*```$", "");
                }

                JsonNode tasksNode = objectMapper.readTree(content);
                if (tasksNode.isArray()) {
                    for (JsonNode node : tasksNode) {
                        tasks.add(AITaskResponse.AITask.builder()
                                .title(node.path("title").asText("Untitled Task"))
                                .description(node.path("description").asText(""))
                                .storyPoints(node.path("storyPoints").asInt(3))
                                .priority(node.path("priority").asText("MEDIUM").toUpperCase())
                                .suggestedRole(node.path("suggestedRole").asText("Developer"))
                                .type(node.path("type").asText("Feature"))
                                .build());
                    }
                }
            }
          }
        } catch (Exception e) {
            log.error("Failed to generate tasks using Groq API: {}", e.getMessage());
        }

        // Fallback to offline template if Groq API fails or returns empty tasks
        if (tasks.isEmpty()) {
            log.info("Falling back to local template task generation.");
            String desc = projectDesc != null ? projectDesc.toLowerCase(Locale.ROOT) : "";
            tasks = detectAndGenerate(desc);
        }

        int totalPts = tasks.stream().mapToInt(AITaskResponse.AITask::getStoryPoints).sum();

        return AITaskResponse.builder()
                .tasks(tasks)
                .totalPoints(totalPts)
                .generatedFor(projectDesc)
                .build();
    }

    private List<AITaskResponse.AITask> detectAndGenerate(String desc) {
        if (desc.contains("ecommerce") || desc.contains("shop") || desc.contains("cart"))
            return ecommerceTasks();
        if (desc.contains("bank") || desc.contains("transfer") || desc.contains("kyc"))
            return bankingTasks();
        if (desc.contains("lms") || desc.contains("learning") || desc.contains("course"))
            return lmsTasks();
        if (desc.contains("food") || desc.contains("delivery") || desc.contains("restaurant"))
            return foodTasks();
        if (desc.contains("social") || desc.contains("feed") || desc.contains("post"))
            return socialTasks();
        return hospitalTasks(); // default
    }

    private List<AITaskResponse.AITask> hospitalTasks() {
        return List.of(
                t("Patient registration & profile management",
                        "Complete patient onboarding with demographics, contact, insurance, and emergency contact details.",
                        8, "CRITICAL", "Developer", "Feature"),
                t("Appointment scheduling engine",
                        "Doctor availability calendar, time slot booking, conflict detection, and automated confirmation flow.",
                        13, "CRITICAL", "Developer", "Feature"),
                t("Electronic Health Records (EHR) module",
                        "Secure, HIPAA-compliant storage of medical history, diagnoses, prescriptions, and lab results.",
                        13, "HIGH", "Developer", "Feature"),
                t("Billing & insurance claim submission",
                        "Invoice generation, insurance API integration, payment processing, and automated remittance.",
                        8, "HIGH", "Developer", "Feature"),
                t("Notification service (SMS & email)",
                        "Appointment reminders, test result alerts, billing notifications via Twilio + SendGrid.", 5,
                        "MEDIUM", "Developer", "Feature"),
                t("HIPAA compliance & audit logging",
                        "Immutable audit trail for all PHI access, automated compliance reports, role-based data masking.",
                        8, "CRITICAL", "Developer", "Security"),
                t("Comprehensive QA & regression suite",
                        "End-to-end test coverage for appointment flows, billing accuracy, and HIPAA compliance edge cases.",
                        8, "HIGH", "Tester", "Testing"));
    }

    private List<AITaskResponse.AITask> ecommerceTasks() {
        return List.of(
                t("Product catalog with Elasticsearch search",
                        "Faceted search, autocomplete, filters by category/price/brand, and sorted results with pagination.",
                        8, "HIGH", "Developer", "Feature"),
                t("Shopping cart & wishlist",
                        "Persistent cart with quantity management, save-for-later, cross-device sync, and promo code support.",
                        5, "HIGH", "Developer", "Feature"),
                t("Multi-step checkout flow",
                        "Address book, shipping method selection, tax calculation, order review, and confirmation email.",
                        13, "CRITICAL", "Developer", "Feature"),
                t("Stripe payment integration",
                        "Card vault, 3DS authentication, refund handling, webhook events, and subscription billing.", 8,
                        "CRITICAL", "Developer", "Integration"),
                t("Inventory & warehouse management",
                        "Real-time stock tracking, low-stock alerts, multi-warehouse routing, and supplier reorder automation.",
                        8, "HIGH", "Developer", "Feature"),
                t("Order management & fulfillment",
                        "Order lifecycle from placement to delivery, status tracking, returns processing, and dispute resolution.",
                        5, "MEDIUM", "Developer", "Feature"),
                t("Performance & load testing",
                        "K6 load tests for 10K concurrent users, payment gateway failures, and inventory race conditions.",
                        8, "HIGH", "Tester", "Testing"));
    }

    private List<AITaskResponse.AITask> bankingTasks() {
        return List.of(
                t("Account dashboard & real-time balance",
                        "Multi-account overview, transaction history with category tagging, and spend analytics.", 8,
                        "HIGH", "Developer", "Feature"),
                t("Fund transfer (NEFT/RTGS/IMPS)",
                        "Domestic and international transfers, beneficiary management, OTP confirmation, SWIFT integration.",
                        13, "CRITICAL", "Developer", "Feature"),
                t("KYC document verification flow",
                        "Aadhaar/PAN OCR, liveness detection, address proof upload, and automated risk scoring.", 8,
                        "CRITICAL", "Developer", "Compliance"),
                t("Biometric & MFA authentication",
                        "Fingerprint, FaceID, TOTP setup, device trust management, and suspicious login detection.", 8,
                        "CRITICAL", "Developer", "Security"),
                t("Loan origination & EMI calculator",
                        "Digital loan applications, credit scoring integration, EMI schedules, and disbursement automation.",
                        5, "MEDIUM", "Developer", "Feature"),
                t("Security penetration testing",
                        "SQL injection, XSS, CSRF, session hijacking, brute-force protection, PCI-DSS compliance.", 13,
                        "CRITICAL", "Tester", "Security"));
    }

    private List<AITaskResponse.AITask> lmsTasks() {
        return List.of(
                t("Course catalog & enrollment",
                        "Browse by category, enroll with seat limits, waitlisting, prerequisites, and learning paths.",
                        5, "HIGH", "Developer", "Feature"),
                t("HLS video player with progress tracking",
                        "Adaptive streaming, bookmarks, speed control, resume position, and offline download.", 8,
                        "HIGH", "Developer", "Feature"),
                t("Quiz & assessment engine",
                        "MCQ, drag-drop, code sandbox, essay grading, and rubric-based scoring with certificates.", 13,
                        "CRITICAL", "Developer", "Feature"),
                t("Live session & webinar module",
                        "WebRTC video rooms, screen sharing, whiteboard, polling, Q&A, and session recordings.", 8,
                        "HIGH", "Developer", "Feature"),
                t("Certificate generation & blockchain anchoring",
                        "Auto-generate PDF certificates on completion with QR verification and optional NFT anchoring.",
                        5, "MEDIUM", "Developer", "Feature"),
                t("Accessibility & WCAG 2.1 compliance testing",
                        "Screen reader, keyboard nav, caption validation, contrast ratios, and mobile a11y audit.", 8,
                        "HIGH", "Tester", "Testing"));
    }

    private List<AITaskResponse.AITask> foodTasks() {
        return List.of(
                t("Restaurant listing with smart filters",
                        "Cuisine, rating, price, ETA, dietary filters with personalized recommendations.", 5, "HIGH",
                        "Developer", "Feature"),
                t("Real-time order tracking (WebSocket)",
                        "Live GPS tracking, animated map, stage-by-stage status updates, and ETA recalculation.", 13,
                        "CRITICAL", "Developer", "Feature"),
                t("Multi-payment integration",
                        "UPI deep links, card vault, COD, wallet credits, split payments, and GST invoicing.", 8,
                        "HIGH", "Developer", "Integration"),
                t("Restaurant partner dashboard",
                        "Order management, menu editor, live capacity controls, revenue analytics, and payout reports.",
                        8, "MEDIUM", "Developer", "Feature"),
                t("Ratings, reviews & photo uploads",
                        "Post-delivery rating modal, reply system, photo moderation, and sentiment analysis.", 5, "LOW",
                        "Developer", "Feature"),
                t("Load & chaos engineering tests",
                        "10K concurrent orders, WebSocket stress tests, GPS drift simulation, and payment failures.", 8,
                        "HIGH", "Tester", "Testing"));
    }

    private List<AITaskResponse.AITask> socialTasks() {
        return List.of(
                t("News feed & infinite scroll",
                        "Algorithmic + chronological feed, pagination, post types (text, image, video, poll).", 8,
                        "HIGH", "Developer", "Feature"),
                t("Notifications center",
                        "Real-time push notifications for likes, comments, mentions, follows via WebSocket + FCM.", 5,
                        "HIGH", "Developer", "Feature"),
                t("Media upload & CDN pipeline",
                        "Multi-image upload, video transcoding, thumbnail generation, and CDN delivery with lazy loading.",
                        8, "HIGH", "Developer", "Integration"),
                t("Search & discovery engine",
                        "Full-text search across posts, users, hashtags with Elasticsearch, trending topics.", 5,
                        "MEDIUM", "Developer", "Feature"),
                t("Content moderation system",
                        "AI-powered toxicity detection, image safety check, report queue, and automated strikes.", 8,
                        "CRITICAL", "Developer", "Safety"),
                t("Performance & abuse prevention tests",
                        "Rate limiting tests, bot detection, spam flood simulation, and CDN failover scenarios.", 5,
                        "HIGH", "Tester", "Testing"));
    }

    private AITaskResponse.AITask t(String title, String desc, int pts, String priority, String role, String type) {
        return AITaskResponse.AITask.builder()
                .title(title).description(desc).storyPoints(pts)
                .priority(priority).suggestedRole(role).type(type).build();
    }

    public void acceptTasks(AcceptAITasksRequest req) {
        Sprint sprint = sprintRepository.findById(req.getSprintId())
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + req.getSprintId()));
        com.flowsync.entity.Project project = sprint.getProject();

        for (AcceptAITasksRequest.AITaskItem item : req.getTasks()) {
            long count = ticketRepository.findByProject_Id(project.getId()).size() + 1;
            String ticketKey = project.getProjectKey() + "-" + (100 + count);

            Ticket ticket = Ticket.builder()
                    .ticketKey(ticketKey)
                    .title(item.getTitle())
                    .description(item.getDescription())
                    .storyPoints(item.getStoryPoints() != null ? item.getStoryPoints() : 3)
                    .priority(Priority.valueOf(item.getPriority().toUpperCase()))
                    .status(TicketStatus.TODO)
                    .project(project)
                    .sprint(sprint)
                    .build();

            ticketRepository.save(ticket);
        }
        com.flowsync.config.WebSocketConfiguration.broadcast("{\"type\": \"TICKET_UPDATED\"}");
    }
}
