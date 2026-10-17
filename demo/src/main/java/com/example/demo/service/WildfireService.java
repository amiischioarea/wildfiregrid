package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class WildfireService {

    private final RegionRepository regionRepository;
    private final WeatherDataRepository weatherDataRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final FireAlertRepository fireAlertRepository;
    private final GridPointRepository gridPointRepository;
    private final ApiLogRepository apiLogRepository;
    private final RestTemplate restTemplate;

    public WildfireService(RegionRepository regionRepository, WeatherDataRepository weatherDataRepository,
                           RiskAssessmentRepository riskAssessmentRepository, FireAlertRepository fireAlertRepository,
                           GridPointRepository gridPointRepository, ApiLogRepository apiLogRepository,
                           RestTemplate restTemplate) {
        this.regionRepository = regionRepository;
        this.weatherDataRepository = weatherDataRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.fireAlertRepository = fireAlertRepository;
        this.gridPointRepository = gridPointRepository;
        this.apiLogRepository = apiLogRepository;
        this.restTemplate = restTemplate;
    }

    @Scheduled(fixedRate = 1800000)
    public void fetchActiveWildfiresAndProcessRisk() {
        System.out.println("🔥 [SATELLITE SYNC] Interogare globală sateliți NASA (Toate continentele, fără cheie)...");

        String eonetUrl = "https://eonet.gsfc.nasa.gov/api/v2.1/categories/8?days=120&limit=80";
        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> response = restTemplate.getForObject(eonetUrl, Map.class);
            apiLogRepository.save(new ApiLog(null, 200, System.currentTimeMillis() - startTime, LocalDateTime.now()));

            if (response != null && response.containsKey("events")) {
                List<Map<String, Object>> events = (List<Map<String, Object>>) response.get("events");

                for (Map<String, Object> event : events) {
                    String eventTitle = (String) event.get("title");
                    List<Map<String, Object>> geometries = (List<Map<String, Object>>) event.get("geometries");

                    if (geometries != null && !geometries.isEmpty()) {
                        Map<String, Object> latestGeo = geometries.get(geometries.size() - 1);
                        double[] latLon = parseLatLon(latestGeo.get("coordinates"));

                        if (latLon != null) {
                            String realCountry = resolveCountryName(latLon[0], latLon[1]);

                            Region region = regionRepository.findByCenterLatitudeAndCenterLongitude(latLon[0], latLon[1])
                                    .orElseGet(() -> regionRepository.save(new Region(eventTitle, latLon[0], latLon[1], realCountry)));

                            if ("Global".equals(region.getCountry()) || "Global Active Hotspot".equals(region.getCountry())) {
                                region.setCountry(realCountry);
                                regionRepository.save(region);
                            }

                            fetchWeatherAndCalculateRisk(region, true);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("error sync: " + e.getMessage());
            apiLogRepository.save(new ApiLog(null, 500, System.currentTimeMillis() - startTime, LocalDateTime.now()));
        }
    }

    private String resolveCountryName(double lat, double lon) {
        try {
            String geoUrl = String.format(Locale.US, "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=%.4f&longitude=%.4f&localityLanguage=en", lat, lon);
            Map<String, Object> geoResponse = restTemplate.getForObject(geoUrl, Map.class);
            if (geoResponse != null && geoResponse.containsKey("countryName")) {
                String cName = (String) geoResponse.get("countryName");
                if (cName != null && !cName.trim().isEmpty()) {
                    return cName;
                }
            }
        } catch (Exception ignored) {}
        return "International Waters / Wildland";
    }

    private double[] parseLatLon(Object coordsObj) {
        try {
            if (coordsObj instanceof List<?> list && list.size() >= 2) {
                if (list.get(0) instanceof Number && list.get(1) instanceof Number) {
                    double lon = ((Number) list.get(0)).doubleValue();
                    double lat = ((Number) list.get(1)).doubleValue();
                    if (lat >= -90.0 && lat <= 90.0 && lon >= -180.0 && lon <= 180.0) return new double[]{lat, lon};
                } else if (!list.isEmpty()) {
                    return parseLatLon(list.get(0));
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void fetchWeatherAndCalculateRisk(Region region, boolean isConfirmedBySatellite) {
        String weatherUrl = String.format(Locale.US, "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m", region.getCenterLatitude(), region.getCenterLongitude());
        long startTime = System.currentTimeMillis();

        try {
            Map<String, Object> response = restTemplate.getForObject(weatherUrl, Map.class);
            apiLogRepository.save(new ApiLog(region, 200, System.currentTimeMillis() - startTime, LocalDateTime.now()));

            if (response != null && response.containsKey("current")) {
                Map<String, Object> current = (Map<String, Object>) response.get("current");
                double temp = ((Number) current.get("temperature_2m")).doubleValue();
                int humidity = ((Number) current.get("relative_humidity_2m")).intValue();
                double windSpeed = ((Number) current.get("wind_speed_10m")).doubleValue();
                int windDir = ((Number) current.get("wind_direction_10m")).intValue();

                weatherDataRepository.save(new WeatherData(region, temp, humidity, windSpeed, windDir, LocalDateTime.now()));

                // Formula corectata fara erori de sintaxa:
                double risk = (windSpeed * 0.40) + (temp * 0.85) + (0.65 * 35.0) + ((100 - humidity) * 0.30) - 10;
                double score = Math.min(Math.max(risk, 0.0), 100.0);

                boolean isFire = isConfirmedBySatellite || (score >= 68.0);

                RiskAssessment assessment = riskAssessmentRepository.save(new RiskAssessment(region, score, isFire, LocalDateTime.now()));
                generateGridPointsForAssessment(assessment, region.getCenterLatitude(), region.getCenterLongitude(), score);

                if (isFire) {
                    fireAlertRepository.save(new FireAlert(region, "CONFIRMED_WILDFIRE_HOTSPOT", "Active detection. Severity index: " + String.format(Locale.US, "%.1f", score) + "%", LocalDateTime.now()));

                    if (score >= 68.0) {
                        sendMobilePushNotification(region.getRegionName(), score);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    @Transactional
    public List<GridPoint> generateGridPointsForAssessment(RiskAssessment assessment, double lat, double lon, double baseRisk) {
        gridPointRepository.deleteByAssessment_AssessmentId(assessment.getAssessmentId());
        List<GridPoint> points = new ArrayList<>();
        Random rand = new Random();

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                double localScore = Math.min(Math.max(baseRisk + (rand.nextDouble() * 6.0 - 3.0), 0.0), 100.0);
                points.add(gridPointRepository.save(new GridPoint(assessment, lat + (i * 0.05), lon + (j * 0.05), localScore)));
            }
        }
        return points;
    }

    // Notificari PUSH (BACKGROUND) PENTRU ANDROID/IOS
    private void sendMobilePushNotification(String regionName, double riskScore) {
        String alertTitle = "🚨 CRITICAL WILDFIRE ALERT";
        String alertBody = String.format(Locale.US, "Extreme fire danger (%.1f%%) detected in %s. Evacuation protocols recommended.", riskScore, regionName);

        try {
            System.out.println("📱 [FCM PUSH NOTIFICATION SENT TO MOBILE DEVICES IN BACKGROUND]");
            System.out.println("TOPIC: wildfire-alerts");
            System.out.println("TITLE: " + alertTitle);
            System.out.println("BODY:  " + alertBody);

        } catch (Exception e) {
            System.err.println("eroare la simularea notificarii Push: " + e.getMessage());
        }
    }
}