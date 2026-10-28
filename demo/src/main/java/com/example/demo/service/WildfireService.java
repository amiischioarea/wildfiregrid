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
    private final UserSubscriptionRepository subscriptionRepository;
    private final RestTemplate restTemplate;

    public WildfireService(RegionRepository regionRepository, WeatherDataRepository weatherDataRepository,
                           RiskAssessmentRepository riskAssessmentRepository, FireAlertRepository fireAlertRepository,
                           GridPointRepository gridPointRepository, ApiLogRepository apiLogRepository,
                           UserSubscriptionRepository subscriptionRepository, RestTemplate restTemplate) {
        this.regionRepository = regionRepository;
        this.weatherDataRepository = weatherDataRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.fireAlertRepository = fireAlertRepository;
        this.gridPointRepository = gridPointRepository;
        this.apiLogRepository = apiLogRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    @Scheduled(fixedRate = 1800000)
    public void fetchActiveWildfiresAndProcessRisk() {
        System.out.println("🔥 [DEMO MODE] Ștergere ierarhică a datelor vechi (pentru a evita blocajele de siguranță)...");
        long startTime = System.currentTimeMillis();

        try {
            // Ștergem Subscripțiile legate de datele demo
            List<UserSubscription> subs = subscriptionRepository.findAll();
            for (UserSubscription s : subs) {
                if (s.getRegion() != null && !"Custom Added".equals(s.getRegion().getCountry())) {
                    subscriptionRepository.delete(s);
                }
            }

            // Ștergem Logurile API legate de datele demo
            List<ApiLog> logs = apiLogRepository.findAll();
            for (ApiLog l : logs) {
                if (l.getRegion() != null && !"Custom Added".equals(l.getRegion().getCountry())) {
                    apiLogRepository.delete(l);
                }
            }

            // Ștergem Punctele de Grilă și Evaluările de Risc
            List<RiskAssessment> assessments = riskAssessmentRepository.findAll();
            for (RiskAssessment a : assessments) {
                if (a.getRegion() != null && !"Custom Added".equals(a.getRegion().getCountry())) {
                    gridPointRepository.deleteByAssessment_AssessmentId(a.getAssessmentId());
                    riskAssessmentRepository.delete(a);
                }
            }

            // Ștergem Alertele
            List<FireAlert> alerts = fireAlertRepository.findAll();
            for (FireAlert fa : alerts) {
                if (fa.getRegion() != null && !"Custom Added".equals(fa.getRegion().getCountry())) {
                    fireAlertRepository.delete(fa);
                }
            }

            // Ștergem Vremea
            List<WeatherData> weathers = weatherDataRepository.findAll();
            for (WeatherData w : weathers) {
                if (w.getRegion() != null && !"Custom Added".equals(w.getRegion().getCountry())) {
                    weatherDataRepository.delete(w);
                }
            }

            // La final, ștergem Regiunile
            List<Region> regions = regionRepository.findAll();
            for (Region r : regions) {
                if (!"Custom Added".equals(r.getCountry())) {
                    regionRepository.delete(r);
                }
            }

            // 2. DETERMINĂM INTERVALUL CURENT PENTRU NOILE SCENARII
            int minute = LocalDateTime.now().getMinute();
            List<MockData> demoLocations = new ArrayList<>();

            if (minute >= 0 && minute < 15) {
                demoLocations.add(new MockData("California Shastina Blaze", 41.3, -122.1, "USA", 39.5, 10, 55.0, 92.5));
                demoLocations.add(new MockData("Amazonas Core Inferno", -3.4, -62.1, "Brazil", 41.0, 40, 15.0, 88.0));
                demoLocations.add(new MockData("Andalusia Dry Zone", 37.3, -5.9, "Spain", 35.0, 25, 20.0, 64.0));
                demoLocations.add(new MockData("British Columbia Advisory", 53.7, -122.6, "Canada", 30.0, 30, 25.0, 58.0));
                demoLocations.add(new MockData("Texas Panhandle Fire", 35.2, -101.8, "USA", 39.0, 10, 50.0, 95.0));
                demoLocations.add(new MockData("Marseille Coastal Brush", 43.2, 5.4, "France", 34.0, 30, 45.0, 71.0));
                demoLocations.add(new MockData("Nevada Desert Edge", 39.0, -117.1, "USA", 42.0, 5, 20.0, 80.0));
                demoLocations.add(new MockData("Pantanal Wetlands Fire", -17.5, -56.5, "Brazil", 38.0, 20, 30.0, 75.0));
                demoLocations.add(new MockData("Oregon Siskiyou", 42.1, -123.5, "USA", 37.0, 15, 35.0, 85.0));
                demoLocations.add(new MockData("Corsica Forest Warning", 42.1, 9.0, "France", 32.0, 40, 20.0, 60.0));
                demoLocations.add(new MockData("Alberta High Prairie", 55.4, -116.4, "Canada", 29.0, 20, 40.0, 69.0));
                demoLocations.add(new MockData("Colorado Front Range", 39.7, -105.5, "USA", 31.0, 12, 55.0, 78.0));
                demoLocations.add(new MockData("Gran Chaco Plain", -22.5, -60.0, "Paraguay", 40.0, 18, 25.0, 82.0));
                demoLocations.add(new MockData("Catalonia Hills", 41.9, 2.2, "Spain", 31.0, 35, 15.0, 55.0));
                demoLocations.add(new MockData("Washington Cascades", 47.7, -121.0, "USA", 30.0, 25, 22.0, 59.0));

            } else if (minute >= 15 && minute < 30) {
                demoLocations.add(new MockData("Rhodes Island Fire", 36.1, 27.9, "Greece", 42.0, 15, 55.0, 96.0));
                demoLocations.add(new MockData("Congo Basin Hotspot", -1.5, 23.8, "DRC", 38.0, 50, 10.0, 82.0));
                demoLocations.add(new MockData("Siberian Taiga Alert", 61.0, 100.0, "Russia", 25.0, 20, 30.0, 72.0));
                demoLocations.add(new MockData("Sicily Dry Brush", 37.5, 14.0, "Italy", 36.0, 35, 15.0, 65.0));
                demoLocations.add(new MockData("Sahara Border Anomaly", 15.0, 5.0, "Niger", 45.0, 5, 20.0, 67.0));
                demoLocations.add(new MockData("Peloponnese Flames", 37.6, 22.0, "Greece", 40.0, 12, 45.0, 89.0));
                demoLocations.add(new MockData("Kruger Park Savanna", -24.0, 31.5, "South Africa", 35.0, 20, 30.0, 74.0));
                demoLocations.add(new MockData("Antalya Tourist Zone", 36.9, 30.7, "Turkey", 39.0, 18, 40.0, 85.0));
                demoLocations.add(new MockData("Sardinia Outback", 40.2, 9.1, "Italy", 34.0, 30, 25.0, 62.0));
                demoLocations.add(new MockData("Tanzania Serengeti", -2.3, 34.8, "Tanzania", 33.0, 40, 15.0, 58.0));
                demoLocations.add(new MockData("Cyprus Troodos", 34.9, 32.8, "Cyprus", 38.0, 22, 35.0, 79.0));
                demoLocations.add(new MockData("Ural Mountains East", 54.0, 59.0, "Russia", 28.0, 25, 45.0, 70.0));
                demoLocations.add(new MockData("Algeria Atlas", 36.2, 3.5, "Algeria", 41.0, 10, 20.0, 81.0));
                demoLocations.add(new MockData("Zambia Woodlands", -13.1, 27.8, "Zambia", 32.0, 35, 18.0, 61.0));
                demoLocations.add(new MockData("Balkan Range Warning", 42.7, 25.3, "Bulgaria", 30.0, 40, 25.0, 56.0));

            } else if (minute >= 30 && minute < 45) {
                demoLocations.add(new MockData("New South Wales Fire", -32.0, 147.0, "Australia", 43.0, 10, 60.0, 98.0));
                demoLocations.add(new MockData("Sumatra Peatlands", 0.5, 101.5, "Indonesia", 36.0, 60, 12.0, 85.0));
                demoLocations.add(new MockData("Cluj Forest Advisory", 46.7, 23.6, "Romania", 32.0, 45, 18.0, 55.0));
                demoLocations.add(new MockData("Portugal Central", 39.5, -8.0, "Portugal", 37.0, 20, 25.0, 66.0));
                demoLocations.add(new MockData("Kangaroo Island Crisis", -35.9, 137.0, "Australia", 40.0, 15, 45.0, 91.0));
                demoLocations.add(new MockData("Kalimantan Bornean", -1.2, 114.0, "Indonesia", 35.0, 65, 10.0, 80.0));
                demoLocations.add(new MockData("Victoria Bushfire", -37.0, 144.0, "Australia", 41.0, 12, 50.0, 94.0));
                demoLocations.add(new MockData("Kyushu Mountain Zone", 32.5, 131.0, "Japan", 29.0, 50, 15.0, 59.0));
                demoLocations.add(new MockData("Mekong Delta Smog", 10.0, 105.5, "Vietnam", 34.0, 55, 20.0, 68.0));
                demoLocations.add(new MockData("Carpathian Ridge", 45.6, 25.6, "Romania", 28.0, 50, 22.0, 54.0));
                demoLocations.add(new MockData("Queensland Outback", -23.0, 144.0, "Australia", 44.0, 8, 30.0, 88.0));
                demoLocations.add(new MockData("Tasmanian Wilderness", -42.0, 146.0, "Australia", 25.0, 30, 45.0, 71.0));
                demoLocations.add(new MockData("Chiang Mai Basin", 18.7, 98.9, "Thailand", 38.0, 25, 12.0, 77.0));
                demoLocations.add(new MockData("Manipur Hills", 24.8, 93.9, "India", 33.0, 40, 15.0, 63.0));
                demoLocations.add(new MockData("Galicia Coast", 42.8, -8.5, "Spain", 30.0, 45, 25.0, 57.0));

            } else {
                demoLocations.add(new MockData("Mato Grosso Fire", -12.6, -55.9, "Brazil", 39.0, 30, 20.0, 78.0));
                demoLocations.add(new MockData("Evia Island Blaze", 38.5, 23.8, "Greece", 41.0, 18, 40.0, 90.0));
                demoLocations.add(new MockData("Alberta Wildlands", 55.0, -115.0, "Canada", 28.0, 25, 35.0, 62.0));
                demoLocations.add(new MockData("California Secondary", 36.7, -119.7, "USA", 35.0, 15, 20.0, 65.0));
                demoLocations.add(new MockData("Madagascar Edge", -18.9, 47.5, "Madagascar", 34.0, 40, 25.0, 75.0));
                demoLocations.add(new MockData("Valparaiso Hills", -33.0, -71.6, "Chile", 36.0, 20, 35.0, 84.0));
                demoLocations.add(new MockData("Yucatan Jungle Alert", 19.3, -89.0, "Mexico", 37.0, 45, 15.0, 67.0));
                demoLocations.add(new MockData("Corsica South Fire", 41.6, 9.2, "France", 38.0, 25, 45.0, 81.0));
                demoLocations.add(new MockData("Angola Central Plains", -12.5, 17.5, "Angola", 33.0, 35, 20.0, 66.0));
                demoLocations.add(new MockData("Florida Everglades", 26.0, -80.6, "USA", 31.0, 50, 15.0, 59.0));
                demoLocations.add(new MockData("Maui Brushfire", 20.7, -156.3, "USA", 33.0, 22, 50.0, 89.0));
                demoLocations.add(new MockData("Pampas Grassland", -35.0, -62.0, "Argentina", 32.0, 30, 40.0, 73.0));
                demoLocations.add(new MockData("Boreal Shield West", 58.0, -105.0, "Canada", 26.0, 25, 30.0, 61.0));
                demoLocations.add(new MockData("Dalmatian Coast", 43.5, 16.5, "Croatia", 35.0, 30, 25.0, 64.0));
                demoLocations.add(new MockData("Kamchatka Volcano Proxy", 55.0, 160.0, "Russia", 20.0, 40, 55.0, 70.0));
            }

            // 3. INJECTAREA DATELOR ÎN BAZĂ
            for (MockData md : demoLocations) {
                Region region = regionRepository.save(new Region(md.name, md.lat, md.lon, md.country));
                weatherDataRepository.save(new WeatherData(region, md.temp, md.humidity, md.wind, 180, LocalDateTime.now()));

                boolean isFire = md.riskScore >= 68.0;
                RiskAssessment assessment = riskAssessmentRepository.save(new RiskAssessment(region, md.riskScore, isFire, LocalDateTime.now()));
                generateGridPointsForAssessment(assessment, md.lat, md.lon, md.riskScore);

                if (isFire) {
                    fireAlertRepository.save(new FireAlert(region, "CONFIRMED_WILDFIRE_HOTSPOT", "Active detection via Demo Mode. Severity index: " + String.format(Locale.US, "%.1f", md.riskScore) + "%", LocalDateTime.now()));
                    sendMobilePushNotification(region.getRegionName(), md.riskScore);
                }
            }

            apiLogRepository.save(new ApiLog(null, 200, System.currentTimeMillis() - startTime, LocalDateTime.now()));
            System.out.println("✅ [DEMO MODE] Baza de date populată cu 15 locații specifice pentru minutul " + minute);

        } catch (Exception e) {
            System.err.println("❌ Eroare la curățarea bazei de date: " + e.getMessage());
        }
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

    private void sendMobilePushNotification(String regionName, double riskScore) {
        String alertTitle = "🚨 CRITICAL WILDFIRE ALERT";
        String alertBody = String.format(Locale.US, "Extreme fire danger (%.1f%%) detected in %s.", riskScore, regionName);
        try {
            System.out.println("📱 [FCM PUSH] " + alertBody);
        } catch (Exception ignored) {}
    }

    private static class MockData {
        String name; double lat; double lon; String country;
        double temp; int humidity; double wind; double riskScore;

        public MockData(String name, double lat, double lon, String country, double temp, int humidity, double wind, double riskScore) {
            this.name = name; this.lat = lat; this.lon = lon; this.country = country;
            this.temp = temp; this.humidity = humidity; this.wind = wind; this.riskScore = riskScore;
        }
    }
}