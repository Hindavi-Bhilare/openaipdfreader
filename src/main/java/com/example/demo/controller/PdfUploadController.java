package com.example.demo.controller;

import com.example.demo.entity.PumpData;
import com.example.demo.repo.PumpDataRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/pdf")
public class PdfUploadController {

    @Value("${openai.api.key}")
    private String openAiKey;

    private final PumpDataRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public PdfUploadController(PumpDataRepository repository) {
        this.repository = repository;
    }

    // ================= MAIN API =================

    @PostMapping("/upload")
    public ResponseEntity<?> uploadPdf(@RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            // 🔹 STEP 1: Extract text from PDF (ONLY IMPORTANT PAGES)
            String pdfText = extractPdfText(file);

            if (pdfText.isBlank()) {
                return ResponseEntity.badRequest().body("No readable text found in PDF");
            }

            // 🔹 STEP 2: Build OpenAI request (SAFE JSON)
            String requestBody = buildOpenAiRequest(pdfText);

            // 🔹 STEP 3: Call OpenAI
            String openAiResponse = callOpenAi(requestBody);

            // 🔹 STEP 4: Extract JSON from OpenAI response
            JsonNode jsonNode = extractJsonFromResponse(openAiResponse);

            // 🔹 STEP 5: Save to DB
            savePumpData(jsonNode);

            return ResponseEntity.ok(jsonNode);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing PDF: " + e.getMessage());
        }
    }

    // ================= PDF TEXT EXTRACTION =================

    private String extractPdfText(MultipartFile file) throws Exception {

        try (PDDocument document = PDDocument.load(file.getInputStream())) {

            PDFTextStripper stripper = new PDFTextStripper();

            // ⚠️ IMPORTANT: Extract only relevant pages (adjust if needed)
            stripper.setStartPage(6);
            stripper.setEndPage(7);

            return stripper.getText(document);
        }
    }

    // ================= BUILD OPENAI REQUEST =================

    private String buildOpenAiRequest(String pdfText) throws Exception {

        Map<String, Object> message = new HashMap<>();

        message.put("role", "user");

        String prompt = """
                Extract ONLY table data from the following text.
                Return STRICT JSON ARRAY with fields:
                scheme_name, total_discharge, each_discharge, pump_head, quantity, pump_type.
                Do NOT add explanation.

                TEXT:
                """ + pdfText;

        message.put("content", prompt);

        Map<String, Object> request = new HashMap<>();
        request.put("model", "gpt-4o");
        request.put("messages", List.of(message));
        request.put("max_tokens", 1500);

        // ✅ SAFE JSON conversion
        return mapper.writeValueAsString(request);
    }

    // ================= CALL OPENAI =================

    private String callOpenAi(String requestBody) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openAiKey);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.postForEntity(
                "https://api.openai.com/v1/chat/completions",
                entity,
                String.class
        );

        return response.getBody();
    }

    // ================= EXTRACT JSON =================

    private JsonNode extractJsonFromResponse(String response) throws Exception {

        JsonNode root = mapper.readTree(response);

        String content = root
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText()
                .trim();

        // Remove markdown if exists
        content = content.replaceAll("```json", "")
                         .replaceAll("```", "")
                         .trim();

        if (!content.startsWith("[") && !content.startsWith("{")) {
            throw new RuntimeException("Invalid JSON from OpenAI: " + content);
        }

        return mapper.readTree(content);
    }

    // ================= SAVE TO DB =================

    private void savePumpData(JsonNode jsonArray) {

        if (!jsonArray.isArray()) {
            throw new RuntimeException("Expected JSON array");
        }

        for (JsonNode node : jsonArray) {

            PumpData data = new PumpData();

            data.setScheme_name(node.path("scheme_name").asText());
            data.setTotal_discharge(node.path("total_discharge").asDouble());
            data.setEach_discharge(node.path("each_discharge").asDouble());
            data.setPump_head(node.path("pump_head").asDouble());
            data.setQuantity(node.path("quantity").asText());
            data.setPump_type(node.path("pump_type").asText());

            repository.save(data);
        }
    }
}