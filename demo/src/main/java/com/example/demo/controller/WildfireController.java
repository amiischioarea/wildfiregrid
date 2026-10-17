package com.example.demo.controller;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.service.WildfireService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class WildfireController {

    private final RegionRepository regionRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final FireAlertRepository fireAlertRepository;
    private final UserRepository userRepository;
    private final UserSubscriptionRepository subscriptionRepository;
    private final GridPointRepository gridPointRepository;
    private final ApiLogRepository apiLogRepository;
    private final WildfireService wildfireService;

    public WildfireController(RegionRepository regionRepository, RiskAssessmentRepository riskAssessmentRepository,
                              FireAlertRepository fireAlertRepository, UserRepository userRepository,
                              UserSubscriptionRepository subscriptionRepository, GridPointRepository gridPointRepository,
                              ApiLogRepository apiLogRepository, WildfireService wildfireService) {
        this.regionRepository = regionRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.fireAlertRepository = fireAlertRepository;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.gridPointRepository = gridPointRepository;
        this.apiLogRepository = apiLogRepository;
        this.wildfireService = wildfireService;
    }

    // rute de baza
    @GetMapping("/users") public List<User> getUsers() { return userRepository.findAll(); }
    @PostMapping("/users") public User createUser(@RequestBody User u) { return userRepository.save(u); }
    @DeleteMapping("/users/{id}") public void deleteUser(@PathVariable Long id) { userRepository.deleteById(id); }

    @GetMapping("/regions") public List<Region> getRegions() { return regionRepository.findAll(); }
    @PostMapping("/regions") public Region createRegion(@RequestBody Region r) { return regionRepository.save(r); }
    @DeleteMapping("/regions/{id}") public void deleteRegion(@PathVariable Long id) { regionRepository.deleteById(id); }

    @GetMapping("/assessments") public List<RiskAssessment> getAllAssessments() { return riskAssessmentRepository.findAll(); }

    @GetMapping("/alerts") public List<FireAlert> getAlerts() { return fireAlertRepository.findAll(); }
    @PostMapping("/alerts") public FireAlert createAlert(@RequestBody FireAlert a) { return fireAlertRepository.save(a); }
    @DeleteMapping("/alerts/{id}") public void deleteAlert(@PathVariable Long id) { fireAlertRepository.deleteById(id); }

    @GetMapping("/subscriptions") public List<UserSubscription> getSubs() { return subscriptionRepository.findAll(); }
    @PostMapping("/subscriptions") public UserSubscription createSub(@RequestBody UserSubscription s) { return subscriptionRepository.save(s); }
    @DeleteMapping("/subscriptions/{id}") public void deleteSub(@PathVariable Long id) { subscriptionRepository.deleteById(id); }

    // operatii complexe
    @GetMapping("/regions/{id}/grid")
    public ResponseEntity<List<GridPoint>> getRegionGridPoints(@PathVariable Long id) {
        Optional<RiskAssessment> latest = riskAssessmentRepository.findAll().stream()
                .filter(a -> a.getRegion().getRegionId().equals(id))
                .max(Comparator.comparing(RiskAssessment::getAssessedAt));

        return latest.map(assessment -> ResponseEntity.ok(gridPointRepository.findByAssessment_AssessmentId(assessment.getAssessmentId())))
                .orElseGet(() -> ResponseEntity.ok(Collections.emptyList()));
    }

    @GetMapping("/assessments/critical")
    public List<RiskAssessment> getCriticalAssessments() {
        return riskAssessmentRepository.findAll().stream()
                .filter(a -> a.getRiskScore() != null && a.getRiskScore() >= 68.0).toList();
    }

    @GetMapping("/notifications/pending")
    public List<User> getUsersNeedingNotifications() {
        return userRepository.findUsersToNotifyForSeverity("CRITICAL_FIRE_EVENT");
    }

    @GetMapping("/logs/performance")
    public List<Object[]> getApiPerformanceStats() {
        return apiLogRepository.findAverageResponseTimeByStatus();
    }

    @PostMapping("/sync/trigger")
    public ResponseEntity<String> triggerManualSync() {
        wildfireService.fetchActiveWildfiresAndProcessRisk();
        return ResponseEntity.ok("Sincronizare executată!");
    }

    // analiza comparativa
    @PostMapping("/ai/analyze")
    public ResponseEntity<Map<String, Object>> getAiTacticalReport(@RequestBody Map<String, Object> weatherData) {
        Map<String, Object> result = new HashMap<>();
        try {
            String regionName = (String) weatherData.getOrDefault("regionName", "Selected Area");
            Object temp = weatherData.get("temp");
            Object hum = weatherData.get("hum");
            Object wind = weatherData.get("wind");
            Object dir = weatherData.get("dir");
            Object formulaRiskObj = weatherData.getOrDefault("risk", 0.0);

            double formulaRisk = 0.0;
            if (formulaRiskObj instanceof Number) {
                formulaRisk = ((Number) formulaRiskObj).doubleValue();
            } else if (formulaRiskObj != null) {
                try {
                    formulaRisk = Double.parseDouble(formulaRiskObj.toString());
                } catch (Exception ignored) {}
            }

            // prompt structurat cu solicitare de răspuns JSON
            String prompt = String.format(Locale.US,
                    "You are an expert wildfire safety AI and fire-behavior analyst. " +
                            "Analyze these real-time weather metrics for area '%s': " +
                            "Temperature: %s°C, Relative Humidity: %s%%, Wind Speed: %s km/h (Direction: %s°). " +
                            "Our deterministic software baseline formula computed a fire risk of %.1f%%. " +
                            "Your tasks: " +
                            " - Independently determine your own AI Risk Score as an estimated number between 0.0 and 100.0 based on non-linear atmospheric factors. " +
                            " - Compare your AI Risk Score with the baseline formula (%.1f%%) and explain any discrepancy in 1 clear sentence. " +
                            " - Provide 2 concise tactical action recommendations for field firefighting units. " +
                            "Respond ONLY with a valid JSON object matching this schema, without code blocks or extra text: " +
                            "{\"aiRisk\": 72.5, \"comparison\": \"Your comparison here\", \"tacticalAdvice\": \"Your tactical recommendations here\"}",
                    regionName, temp, hum, wind, dir, formulaRisk, formulaRisk
            );

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama3.2");
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("format", "json");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity("http://localhost:11434/api/generate", entity, Map.class);

            String rawResponse = "";
            if (response.getBody() != null && response.getBody().containsKey("response")) {
                rawResponse = (String) response.getBody().get("response");
            }

            double aiRisk = formulaRisk;
            String comparison = "AI assessment closely mirrors baseline atmospheric computation.";
            String tacticalAdvice = "Maintain routine perimeter observation.";

            // extragere cu expresii regulate
            try {
                Pattern riskPattern = Pattern.compile("\"aiRisk\"\\s*:\\s*([0-9.]+)");
                Matcher riskMatcher = riskPattern.matcher(rawResponse);
                if (riskMatcher.find()) {
                    aiRisk = Double.parseDouble(riskMatcher.group(1));
                }

                Pattern compPattern = Pattern.compile("\"comparison\"\\s*:\\s*\"([^\"]+)\"");
                Matcher compMatcher = compPattern.matcher(rawResponse);
                if (compMatcher.find()) {
                    comparison = compMatcher.group(1);
                }

                Pattern advicePattern = Pattern.compile("\"tacticalAdvice\"\\s*:\\s*\"([^\"]+)\"");
                Matcher adviceMatcher = advicePattern.matcher(rawResponse);
                if (adviceMatcher.find()) {
                    tacticalAdvice = adviceMatcher.group(1);
                }
            } catch (Exception parseEx) {
                if (!rawResponse.isEmpty()) tacticalAdvice = rawResponse;
            }

            double delta = aiRisk - formulaRisk;
            double hybridScore = (formulaRisk * 0.5) + (aiRisk * 0.5);

            double roundedFormula = Math.round(formulaRisk * 10.0) / 10.0;
            double roundedAiRisk = Math.round(aiRisk * 10.0) / 10.0;
            double roundedDelta = Math.round(delta * 10.0) / 10.0;
            double roundedHybrid = Math.round(hybridScore * 10.0) / 10.0;

            result.put("softwareRisk", roundedFormula);
            result.put("aiRisk", roundedAiRisk);
            result.put("delta", roundedDelta);
            result.put("hybridScore", roundedHybrid);
            result.put("comparison", comparison);
            result.put("tacticalAdvice", tacticalAdvice);

            String combinedReport = String.format(Locale.US,
                    "📊 Formula Risk: %.1f%% | 🤖 AI Risk: %.1f%% (Divergence: %+.1f%% | Hybrid Consensus: %.1f%%)\n\n" +
                            "🔍 Analysis Comparison: %s\n\n" +
                            "📋 Tactical Advice: %s",
                    roundedFormula, roundedAiRisk, roundedDelta, roundedHybrid, comparison, tacticalAdvice
            );
            result.put("report", combinedReport);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            result.put("softwareRisk", weatherData.getOrDefault("risk", 0.0));
            result.put("aiRisk", 0.0);
            result.put("delta", 0.0);
            result.put("hybridScore", weatherData.getOrDefault("risk", 0.0));
            result.put("comparison", "Could not query Llama 3.2 model.");
            result.put("tacticalAdvice", "Ensure Ollama is running on port 11434.");
            result.put("report", "❌ Error connecting to Ollama. Make sure Ollama and llama3.2 are running on port 11434.");
            return ResponseEntity.ok(result);
        }
    }
}