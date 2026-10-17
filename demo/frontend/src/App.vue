<template>
  <div class="app-container">
    <div class="dashboard-layout">
      <!-- SIDEBAR CONTROL PANEL -->
      <aside class="sidebar">
        <h2 class="panel-title">⚙ System Control Panel</h2>
        <div class="divider"></div>

        <label class="panel-label">🗺️ Select Base Layer:</label>
        <select v-model="mapStyle" @change="updateMapStyle" class="custom-select">
          <option value="satellite">🇪🇺 Copernicus Sentinel-2</option>
          <option value="topo">🏔 OpenTopoMap</option>
        </select>

        <!-- SELECTOR CONTINENT -->
        <label class="panel-label" style="margin-top: 15px; display: block;">🌍 Focus Continent / Region:</label>
        <select v-model="selectedContinent" @change="handleContinentChange" class="custom-select" style="border-color: #00d2ff;">
          <option value="all">🌐 Global View (All)</option>
          <option value="europe">🌏 Europe</option>
          <option value="north_america">🌏 North America</option>
          <option value="south_america">🌏 South America</option>
          <option value="africa">🌍 Africa</option>
          <option value="asia">🌏 Asia</option>
          <option value="australia">🌏 Australia & Oceania</option>
        </select>

        <div class="divider"></div>
        <h3 class="panel-title">📍 Manual Coordinates</h3>
        <p style="font-size: 11px; color: #8b949e; margin-bottom: 5px;">Enter custom GPS coordinates to scan and monitor.</p>

        <input v-model="manualRegionName" placeholder="Region Name (e.g. My Forest)" class="custom-input" style="margin-bottom: 5px;" />

        <div style="display: flex; gap: 5px; margin-bottom: 5px;">
          <input v-model="manualLat" type="number" step="0.0001" placeholder="Lat (ex: 46.77)" class="custom-input" style="margin: 0;" />
          <input v-model="manualLon" type="number" step="0.0001" placeholder="Lon (ex: 23.59)" class="custom-input" style="margin: 0;" />
        </div>

        <button class="action-btn" style="background-color: #d9d24a; color: black !important; border: none;" @click="scanManualCoordinates">
          🔍 Scan Area
        </button>
        <button class="action-btn sync-btn" style="margin-top: 5px;" @click="saveManualRegion">
          💾 Save to Regions DB
        </button>

        <div class="divider"></div>

        <button class="action-btn sync-btn" @click="triggerBackendSync">
          📡 Search Fires (NASA)
        </button>
        <button class="action-btn" @click="resetMap">🗑️ Reset Map View</button>
        <button class="action-btn" @click="fetchHotspots">🔄 Refresh Live Telemetry</button>

        <!-- BUTON SETARI / CONT -->
        <button class="action-btn" style="background-color: #2ea043; border-color: #3fb950; margin-top: 20px;" @click="showSettingsModal = true">
          👤 {{ currentUser ? currentUser.fullName || currentUser.email : 'My Account / Settings' }}
        </button>

        <div class="divider"></div>
        <h3 class="panel-title">💡 Wildfire Risk Scale</h3>
        <p class="risk-scale-text" style="color: #00ff66;">🟩 0 - 30%: Low Baseline Risk</p>
        <p class="risk-scale-text" style="color: #ffcc00;">🟨 30 - 50%: Moderate Risk</p>
        <p class="risk-scale-text" style="color: #ff6600;">🟧 50 - 68%: High Risk Warning</p>
        <p class="risk-scale-text" style="color: #ff0033;">🟥 68 - 100%: Critical / Fire</p>
      </aside>

      <!-- MAIN MAP AREA -->
      <main class="map-container">
        <div id="map"></div>

        <div v-if="latestAlert" class="live-alert-overlay">
          🚨 LIVE ALERT: {{ latestAlert.region?.regionName || 'Region' }} | Risk: {{ latestAlert.severityLevel }}
        </div>
      </main>

      <!-- RIGHT TELEMETRY PANEL -->
      <aside class="telemetry-panel">
        <h2 class="panel-title" v-if="!selectedRegion">📈 System Overview</h2>
        <h2 class="panel-title" v-else>📊 Point Telemetry</h2>

        <div v-if="!selectedRegion">
          <div class="summary-card">
            <p class="card-subtitle">ACTIVE FIRES ({{ selectedContinent.toUpperCase() }})</p>
            <h2 class="card-value" style="color: #ff0033;">{{ confirmedFires.length }}</h2>
          </div>
          <div class="summary-card">
            <p class="card-subtitle">HIGH RISK ADVISORY</p>
            <h2 class="card-value" style="color: #ffcc00;">{{ warningZones.length }}</h2>
          </div>
          <p class="helper-text">
            Click anywhere on the map (or on the wind arrows) to generate a local weather data grid.
          </p>
        </div>

        <div v-else class="telemetry-details">
          <div class="selected-coords">
            📍 <b>{{ selectedRegion.regionName }}</b>
          </div>
          <div class="weather-box">
            <p class="weather-title">🌡️ Meteo Data (Open-Meteo):</p>
            <ul>
              <li>Lat: {{ selectedRegion.centerLatitude ? selectedRegion.centerLatitude.toFixed(3) : 'N/A' }} | Lon: {{ selectedRegion.centerLongitude ? selectedRegion.centerLongitude.toFixed(3) : 'N/A' }}</li>
              <template v-if="selectedWeather">
                <li>Temp: <b>{{ selectedWeather.temp }} °C</b></li>
                <li>Humidity: <b>{{ selectedWeather.hum }} %</b></li>
                <li>Wind: <b>{{ selectedWeather.wind }} km/h</b> (Dir: {{ selectedWeather.dir }}°)</li>
              </template>
            </ul>
          </div>
          <div class="summary-card">
            <p class="card-subtitle">Maximum Risk Found in Grid</p>
            <h2 class="card-value" :style="{ color: selectedRisk >= 68 ? '#ff0033' : (selectedRisk >= 40 ? '#ffcc00' : '#00ff66') }">
              {{ selectedRisk }}%
            </h2>
          </div>

          <!-- SECȚIUNE ANALIZĂ COMPARATIVĂ AI VS FORMULĂ -->
          <div style="margin-top: 15px; border-top: 1px solid #2d3545; padding-top: 15px;">
            <button class="action-btn" style="background-color: #6f42c1; border-color: #8a53e6;" @click="generateAiReport" :disabled="loadingAi">
              {{ loadingAi ? '🤖 Neural Assessment in progress...' : '🤖 Run AI vs Formula Audit' }}
            </button>

            <div v-if="aiAnalysis" class="comparison-card" style="margin-top: 15px; background: #161528; border: 1px solid #8a53e6; border-radius: 8px; padding: 12px;">
              <h4 style="color: #c77dff; font-size: 14px; margin-bottom: 10px;">⚖ Dual-Risk Assessment</h4>

              <div style="display: flex; justify-content: space-between; gap: 10px; margin-bottom: 10px;">
                <div style="flex: 1; background: #0e121a; padding: 8px; border-radius: 5px; text-align: center;">
                  <span style="font-size: 11px; color: #8b949e; display: block;">Formula Model</span>
                  <b style="font-size: 18px; color: #58a6ff;">{{ aiAnalysis.softwareRisk }}%</b>
                </div>
                <div style="flex: 1; background: #0e121a; padding: 8px; border-radius: 5px; text-align: center;">
                  <span style="font-size: 11px; color: #8b949e; display: block;">AI Neural Model</span>
                  <b style="font-size: 18px; color: #c77dff;">{{ aiAnalysis.aiRisk }}%</b>
                </div>
              </div>

              <div style="background: #0b0e14; padding: 8px; border-radius: 5px; margin-bottom: 10px; font-size: 12px;">
                <p style="margin: 0; display: flex; justify-content: space-between;">
                  <span>Divergence (&Delta;):</span>
                  <b :style="{ color: Math.abs(aiAnalysis.delta) > 15 ? '#ff0033' : '#00ff66' }">
                    {{ aiAnalysis.delta > 0 ? '+' : '' }}{{ aiAnalysis.delta }}%
                  </b>
                </p>
                <p style="margin: 4px 0 0 0; display: flex; justify-content: space-between;">
                  <span>Hybrid Consensus:</span>
                  <b style="color: #ffcc00;">{{ aiAnalysis.hybridScore }}%</b>
                </p>
              </div>

              <div style="margin-bottom: 8px;">
                <p style="font-size: 11px; color: #8b949e; margin-bottom: 2px;">🔍 Model Discrepancy Rationale:</p>
                <p style="font-size: 12px; color: #e2d9f3; line-height: 1.3; font-style: italic;">
                  "{{ aiAnalysis.comparison }}"
                </p>
              </div>

              <div>
                <p style="font-size: 11px; color: #8b949e; margin-bottom: 2px;">📋 Tactical Recommendation:</p>
                <p style="font-size: 12px; color: #ffffff; line-height: 1.3;">
                  {{ aiAnalysis.tacticalAdvice }}
                </p>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>

    <!-- LOWER INCIDENT REPORT SECTION -->
    <div class="incident-logs">
      <div class="log-column">
        <h3 class="panel-title">🔴 Active Incidents ({{ filteredHotspots.length }})</h3>
        <div class="cards-container">
          <div v-for="fire in confirmedFires" :key="fire.regionId" class="fire-confirmed-card">
            <h4>🔥 {{ fire.regionName }}</h4>
            <p>• Status: Active hotspot detected by satellites.</p>
            <p>• Lat: {{ fire.centerLatitude != null ? fire.centerLatitude.toFixed(2) : 'N/A' }} | Lon: {{ fire.centerLongitude != null ? fire.centerLongitude.toFixed(2) : 'N/A' }}</p>
          </div>
          <p v-if="confirmedFires.length === 0" style="color: #00ff66;">✅ No fire events in this continent.</p>
        </div>
      </div>

      <div class="log-column">
        <h3 class="panel-title">🟧 High Risk Advisory Zones</h3>
        <div class="cards-container">
          <div v-for="warn in warningZones" :key="warn.regionId" class="fire-warning-card">
            <h4 style="color: #ffcc00; margin-bottom: 5px;">⚠️ {{ warn.regionName }}</h4>
            <p style="font-size: 13px; color: #d0d7de; margin: 2px 0;">• Status: Increased risk of ignition.</p>
            <p style="font-size: 13px; color: #d0d7de; margin: 2px 0;">• Lat: {{ warn.centerLatitude != null ? warn.centerLatitude.toFixed(2) : 'N/A' }} | Lon: {{ warn.centerLongitude != null ? warn.centerLongitude.toFixed(2) : 'N/A' }}</p>
          </div>
          <p v-if="warningZones.length === 0" style="color: #8b949e;">No secondary risk advisory zones in this area.</p>
        </div>
      </div>
    </div>

    <!-- FEREASTRA MODALĂ PENTRU SETĂRI / CONT -->
    <div v-if="showSettingsModal" class="modal-overlay" @click.self="showSettingsModal = false">
      <div class="modal-content">
        <button class="close-btn" @click="showSettingsModal = false">✖</button>
        <h2 style="margin-bottom: 20px; color: #00d2ff;">👤 My Account & Settings</h2>

        <div v-if="!currentUser">
          <div style="display: flex; gap: 10px; margin-bottom: 20px;">
            <button
                :style="{ backgroundColor: authMode === 'login' ? '#004080' : '#1e2636' }"
                class="action-btn"
                style="margin: 0;"
                @click="authMode = 'login'">
              🔑 Log In
            </button>
            <button
                :style="{ backgroundColor: authMode === 'register' ? '#004080' : '#1e2636' }"
                class="action-btn"
                style="margin: 0;"
                @click="authMode = 'register'">
              📝 Sign Up
            </button>
          </div>

          <div v-if="authMode === 'login'">
            <p style="margin-bottom: 15px; font-size: 13px; color: #d0d7de;">Enter the email address you used to register previously:</p>
            <input v-model="loginEmail" placeholder="Email Address" class="custom-input" />
            <button class="action-btn sync-btn" style="margin-top: 10px;" @click="loginUser">🔓 Log In</button>
          </div>

          <div v-else>
            <p style="margin-bottom: 15px; font-size: 13px; color: #d0d7de;">Create a new account in the database:</p>
            <input v-model="userForm.fullName" placeholder="Full Name" class="custom-input" />
            <input v-model="userForm.email" placeholder="Email Address" class="custom-input" />
            <input v-model="userForm.passwordHash" type="password" placeholder="Password" class="custom-input" />
            <button class="action-btn sync-btn" style="margin-top: 10px;" @click="createUser">✅ Sign Up</button>
          </div>
        </div>

        <div v-else>
          <h3 style="color: #00ff66;">Hello, {{ currentUser.fullName || 'User' }}!</h3>
          <p style="font-size: 13px; color: #8b949e; margin-bottom: 20px;">Email: {{ currentUser.email }}</p>

          <div class="divider"></div>
          <h3 style="margin-bottom: 10px;">🔔 Add an Alert Subscription</h3>
          <p style="font-size: 13px; color: #d0d7de; margin-bottom: 10px;">Select a region detected live or enter a new one:</p>

          <div style="margin-bottom: 15px;">
            <label style="font-size: 12px; color: #8b949e;">Select a region from the live hotspots:</label>
            <select v-model="subForm.selectedRegionId" class="custom-select" @change="subForm.customRegionName = ''">
              <option value="" disabled selected>-- Select from active hotspots --</option>
              <option v-for="h in filteredHotspots" :key="h.regionId" :value="h.regionId">
                {{ h.regionName }}
              </option>
            </select>
          </div>

          <div style="margin-bottom: 15px;">
            <label style="font-size: 12px; color: #8b949e;">Or enter a custom region/country name:</label>
            <input v-model="subForm.customRegionName" placeholder="ex: Romania, California, Attica..." class="custom-input" @input="subForm.selectedRegionId = ''" />
          </div>

          <div style="display: flex; align-items: center; margin-bottom: 10px;">
            <input type="checkbox" id="notifyEmail" v-model="subForm.notifyByEmail" style="margin-right: 10px; width: 18px; height: 18px;" />
            <label for="notifyEmail" style="cursor: pointer; color: white;">Send me email alerts</label>
          </div>

          <button class="action-btn" style="background-color: #2ea043; border-color: #3fb950; margin-top: 15px;" @click="createSubscription">
            ➕ Save Subscription
          </button>

          <div class="divider"></div>
          <button class="action-btn" style="background-color: #3b1219; border-color: #ff0033;" @click="logoutUser">
            🚪 Disconnect
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import axios from 'axios';
import { Client } from '@stomp/stompjs';

const map = ref(null);
const currentLayer = ref(null);
const mapStyle = ref('satellite');
const selectedContinent = ref('all');
const gridLayers = ref(L.layerGroup());
const markerLayers = ref(L.layerGroup());
const windLayers = ref(L.layerGroup());

const hotspots = ref([]);
const latestAlert = ref(null);
const selectedRegion = ref(null);
const selectedWeather = ref(null);
const selectedRisk = ref(0);

const manualRegionName = ref('');
const manualLat = ref(null);
const manualLon = ref(null);

const continentConfigs = {
  all: { center: [20.0, 0.0], zoom: 2, filter: () => true },
  europe: {
    center: [50.0, 15.0],
    zoom: 4,
    filter: (lat, lon) => lat >= 35 && lat <= 71 && lon >= -25 && lon <= 45
  },
  north_america: {
    center: [40.0, -100.0],
    zoom: 3,
    filter: (lat, lon) => lat >= 15 && lat <= 72 && lon >= -170 && lon <= -50
  },
  south_america: {
    center: [-15.0, -60.0],
    zoom: 3,
    filter: (lat, lon) => lat >= -56 && lat <= 13 && lon >= -82 && lon <= -34
  },
  africa: {
    center: [5.0, 20.0],
    zoom: 3,
    filter: (lat, lon) => lat >= -35 && lat <= 38 && lon >= -18 && lon <= 52
  },
  asia: {
    center: [40.0, 95.0],
    zoom: 3,
    filter: (lat, lon) => lat >= 5 && lat <= 75 && lon >= 45 && lon <= 150
  },
  australia: {
    center: [-25.0, 135.0],
    zoom: 4,
    filter: (lat, lon) => lat >= -48 && lat <= -10 && lon >= 110 && lon <= 180
  }
};

const filteredHotspots = computed(() => {
  const cfg = continentConfigs[selectedContinent.value] || continentConfigs.all;
  return hotspots.value.filter(h => {
    if (h.centerLatitude == null || h.centerLongitude == null) return false;
    return cfg.filter(h.centerLatitude, h.centerLongitude);
  });
});

const confirmedFires = computed(() => filteredHotspots.value.slice(0, Math.ceil(filteredHotspots.value.length / 2)));
const warningZones = computed(() => filteredHotspots.value.slice(Math.ceil(filteredHotspots.value.length / 2)));

const aiAnalysis = ref(null);
const loadingAi = ref(false);

const showSettingsModal = ref(false);
const authMode = ref('login');
const loginEmail = ref('');
const currentUser = ref(null);

const userForm = ref({
  fullName: '',
  email: '',
  passwordHash: '',
  userRole: 'USER'
});

const subForm = ref({
  selectedRegionId: '',
  customRegionName: '',
  notifyByEmail: true
});

onMounted(() => {
  const savedUser = localStorage.getItem('wildfire_user');
  if (savedUser) {
    try {
      currentUser.value = JSON.parse(savedUser);
    } catch (e) {
      localStorage.removeItem('wildfire_user');
    }
  }
  initMap();
  setupWebSockets();
  fetchHotspots();
});

function handleContinentChange() {
  const cfg = continentConfigs[selectedContinent.value] || continentConfigs.all;
  if (map.value) {
    map.value.flyTo(cfg.center, cfg.zoom, { animate: true, duration: 1.5 });
  }
  renderHotspotMarkers();
}

function renderHotspotMarkers() {
  markerLayers.value.clearLayers();
  filteredHotspots.value.forEach(region => {
    if (region.centerLatitude != null && region.centerLongitude != null) {
      const fireIcon = L.divIcon({
        html: `<div style="font-size: 28px; text-shadow: 0 0 8px red; cursor: pointer;">🔥</div>`,
        className: 'custom-icon',
        iconSize: [30, 30],
        iconAnchor: [15, 15]
      });
      const marker = L.marker([region.centerLatitude, region.centerLongitude], { icon: fireIcon })
          .addTo(markerLayers.value)
          .bindTooltip(region.regionName || 'Incident', { permanent: false, direction: 'top' });
      marker.on('click', () => {
        generateDynamicGrid(region.centerLatitude, region.centerLongitude);
      });
    }
  });
}

function scanManualCoordinates() {
  if (!manualLat.value || !manualLon.value) {
    alert("Please enter both Latitude and Longitude!");
    return;
  }
  const lat = parseFloat(manualLat.value);
  const lon = parseFloat(manualLon.value);

  generateDynamicGrid(lat, lon);
  map.value.flyTo([lat, lon], 10, { animate: true, duration: 1.5 });
}

function saveManualRegion() {
  if (!manualRegionName.value || !manualLat.value || !manualLon.value) {
    alert("Please fill in the Name, Latitude, and Longitude to save!");
    return;
  }

  const payload = {
    regionName: manualRegionName.value,
    centerLatitude: parseFloat(manualLat.value),
    centerLongitude: parseFloat(manualLon.value),
    country: "Custom Added"
  };

  axios.post('http://localhost:8080/api/v1/regions', payload)
      .then(res => {
        alert(`Region "${manualRegionName.value}" saved successfully!`);
        manualRegionName.value = '';
        manualLat.value = null;
        manualLon.value = null;
        fetchHotspots();
      })
      .catch(err => {
        console.error(err);
        alert("Error saving region to database.");
      });
}

function generateAiReport() {
  if (!selectedRegion.value || !selectedWeather.value) {
    alert("Please select a location on the map first!");
    return;
  }

  loadingAi.value = true;
  aiAnalysis.value = null;

  const payload = {
    regionName: selectedRegion.value.regionName,
    temp: selectedWeather.value.temp,
    hum: selectedWeather.value.hum,
    wind: selectedWeather.value.wind,
    dir: selectedWeather.value.dir,
    risk: parseFloat(selectedRisk.value)
  };

  axios.post('http://localhost:8080/api/v1/ai/analyze', payload)
      .then(res => {
        aiAnalysis.value = res.data;
      })
      .catch(err => {
        console.error(err);
        alert("❌ Error communicating with AI service. Check Ollama.");
      })
      .finally(() => {
        loadingAi.value = false;
      });
}

function loginUser() {
  if (!loginEmail.value) {
    alert("Please enter your email address!");
    return;
  }

  axios.get('http://localhost:8080/api/v1/users')
      .then(res => {
        const users = res.data;
        const found = users.find(u => u.email.toLowerCase().trim() === loginEmail.value.toLowerCase().trim());

        if (found) {
          currentUser.value = found;
          localStorage.setItem('wildfire_user', JSON.stringify(found));
          alert(`You have successfully logged in! Hello, ${found.fullName || found.email}!`);
        } else {
          alert("I couldn't find an account with this email address. Create a new account!");
        }
      })
      .catch(err => {
        alert("Login error. Check if the server is started.");
        console.error(err);
      });
}

function createUser() {
  if(!userForm.value.fullName || !userForm.value.email || !userForm.value.passwordHash) {
    alert("Please fill in your name, email, and password!");
    return;
  }

  const payload = {
    fullName: userForm.value.fullName,
    email: userForm.value.email,
    passwordHash: userForm.value.passwordHash,
    userRole: userForm.value.userRole
  };

  axios.post('http://localhost:8080/api/v1/users', payload)
      .then(res => {
        currentUser.value = res.data;
        localStorage.setItem('wildfire_user', JSON.stringify(res.data));
        alert("Account successfully created!");
      })
      .catch(err => {
        alert("Error creating the account! Check the IntelliJ console.");
        console.error(err);
      });
}

function logoutUser() {
  currentUser.value = null;
  localStorage.removeItem('wildfire_user');
}

function createSubscription() {
  if (!subForm.value.selectedRegionId && !subForm.value.customRegionName) {
    alert("Please select a region from the list OR enter a region name manually!");
    return;
  }

  const idUtilizator = currentUser.value.userId || currentUser.value.user_id || currentUser.value.id;

  let targetRegionName = subForm.value.customRegionName;
  if (subForm.value.selectedRegionId) {
    const found = filteredHotspots.value.find(h => h.regionId === subForm.value.selectedRegionId);
    if (found) targetRegionName = found.regionName;
  }

  const payload = {
    user: {
      userId: idUtilizator
    },
    region: subForm.value.selectedRegionId ? { regionId: subForm.value.selectedRegionId } : null,
    regionNameKeyword: targetRegionName,
    notifyByEmail: subForm.value.notifyByEmail
  };

  axios.post('http://localhost:8080/api/v1/subscriptions', payload)
      .then(res => {
        alert(`You have successfully subscribed to alerts for "${targetRegionName}"!`);
        subForm.value.selectedRegionId = '';
        subForm.value.customRegionName = '';
      })
      .catch(err => {
        alert("Error adding the subscription. Check the IntelliJ console.");
        console.error("Error details:", err.response?.data || err.message);
      });
}

function initMap() {
  map.value = L.map('map', { zoomControl: true, minZoom: 2 }).setView([20.0, 0.0], 2);
  setTimeout(() => { map.value.invalidateSize(); }, 500);

  gridLayers.value.addTo(map.value);
  markerLayers.value.addTo(map.value);
  windLayers.value.addTo(map.value);
  updateMapStyle();

  map.value.on('moveend', drawWindGrid);
  drawWindGrid();

  map.value.on('click', (e) => {
    generateDynamicGrid(e.latlng.lat, e.latlng.lng);
  });
}

function updateMapStyle() {
  if (currentLayer.value) map.value.removeLayer(currentLayer.value);
  if (mapStyle.value === 'satellite') {
    currentLayer.value = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', { maxZoom: 19 }).addTo(map.value);
  } else {
    currentLayer.value = L.tileLayer('https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', { maxZoom: 17 }).addTo(map.value);
  }
}

function drawWindGrid() {
  windLayers.value.clearLayers();
  const bounds = map.value.getBounds();
  const zoom = map.value.getZoom();
  if(zoom < 3) return;

  const latStep = (bounds.getNorth() - bounds.getSouth()) / 12;
  const lonStep = (bounds.getEast() - bounds.getWest()) / 16;

  for (let lat = bounds.getSouth() + latStep/2; lat < bounds.getNorth(); lat += latStep) {
    for (let lon = bounds.getWest() + lonStep/2; lon < bounds.getEast(); lon += lonStep) {
      const speed = (Math.sin(lat * 5) * Math.cos(lon * 5) + 1) * 20 + 10;
      let dir = (Math.sin(lon * 2) + Math.cos(lat * 2)) * 180 + 180;
      dir = ((dir % 360) + 360) % 360;

      let color = "#4ad9e0";
      if(speed > 25) color = "#4ad966";
      if(speed > 35) color = "#d9d24a";
      if(speed > 45) color = "#e0473c";

      const arrowHtml = `<div style="transform: rotate(${dir}deg); color: ${color}; font-size: 20px; font-weight: bold; text-shadow: 0px 0px 3px black; cursor: pointer;">↑</div>`;
      const icon = L.divIcon({ html: arrowHtml, className: '', iconSize: [20, 20] });

      const marker = L.marker([lat, lon], { icon: icon, interactive: true }).addTo(windLayers.value);
      marker.bindTooltip(`🌬️ Wind: ${speed.toFixed(1)} km/h<br>🧭 Dir: ${dir.toFixed(0)}°`, {
        direction: 'top', offset: [0, -10], opacity: 0.9, className: 'wind-hover-tooltip'
      });
      marker.on('click', () => { generateDynamicGrid(lat, lon); });
    }
  }
}

function generateDynamicGrid(lat, lon) {
  axios.get(`https://api.open-meteo.com/v1/forecast?latitude=${lat}&longitude=${lon}&current=temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m`)
      .then(res => {
        const current = res.data.current;
        selectedRegion.value = { regionName: manualRegionName.value || "Manually Scanned Point", centerLatitude: lat, centerLongitude: lon };
        selectedWeather.value = { temp: current.temperature_2m, hum: current.relative_humidity_2m, wind: current.wind_speed_10m, dir: current.wind_direction_10m };

        const dryness = 0.65;
        let baseRisk = (current.wind_speed_10m * 0.40) + (current.temperature_2m * 0.85) + (dryness * 35.0) + ((100 - current.relative_humidity_2m) * 0.30) - 10;

        gridLayers.value.clearLayers();
        let maxRisk = 0;
        const step = 0.05;

        for (let i = -2; i <= 2; i++) {
          for (let j = -2; j <= 2; j++) {
            const pLat = lat + (i * step);
            const pLon = lon + (j * step);
            const localRisk = Math.min(Math.max(baseRisk + (Math.random() * 6 - 3), 0), 100);
            if(localRisk > maxRisk) maxRisk = localRisk;

            let color = '#ffcc00';
            if(localRisk >= 68) color = '#ff0033';
            if(localRisk < 40) color = '#00ff66';

            const bounds = [[pLat - step/2, pLon - step/2], [pLat + step/2, pLon + step/2]];
            const rect = L.rectangle(bounds, { color: '#ffffff', weight: 1, fillColor: color, fillOpacity: 0.55 });
            rect.bindPopup(`<div class="popup-content"><b>Local Cell Data:</b><br>Risk: <b style="color:${color}">${localRisk.toFixed(1)}%</b><br>Temp: ${current.temperature_2m}°C<br>Wind: ${current.wind_speed_10m} km/h<br>Humidity: ${current.relative_humidity_2m}%</div>`);
            gridLayers.value.addLayer(rect);
          }
        }
        selectedRisk.value = maxRisk.toFixed(1);
        map.value.flyTo([lat, lon], 9, { animate: true, duration: 1.0 });
      })
      .catch(err => alert("Error retrieving weather data for this location."));
}

function triggerBackendSync() {
  alert("Querying NASA satellites... Please wait (this may take a few seconds).");
  axios.post('http://localhost:8080/api/v1/sync/trigger')
      .then(response => {
        alert("Full synchronization successful! Data is loading onto the map.");
        fetchHotspots();
      })
      .catch(error => {
        alert("❌ Error communicating with the backend server! Check the IntelliJ console.");
      });
}

function setupWebSockets() {
  const stompClient = new Client({
    brokerURL: 'ws://localhost:8080/ws-wildfire',
    onConnect: () => {
      stompClient.subscribe('/topic/alerts', (message) => {
        latestAlert.value = JSON.parse(message.body);
        setTimeout(() => latestAlert.value = null, 8000);
        fetchHotspots();
      });
    }
  });
  stompClient.activate();
}

function fetchHotspots() {
  axios.get('http://localhost:8080/api/v1/regions')
      .then(response => {
        hotspots.value = response.data || [];
        renderHotspotMarkers();
      })
      .catch(err => {
        console.error("Fetch hotspots error:", err);
      });
}

function resetMap() {
  selectedRegion.value = null;
  selectedWeather.value = null;
  selectedRisk.value = 0;
  aiAnalysis.value = null;
  selectedContinent.value = 'all';
  manualRegionName.value = '';
  manualLat.value = null;
  manualLon.value = null;

  gridLayers.value.clearLayers();
  markerLayers.value.clearLayers();
  hotspots.value = [];
  map.value.setView([20.0, 0.0], 2);
}
</script>

<style>
* { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }
body { background-color: #0b0e14; overflow: hidden; }
h2, h3, h4, p, label, li, b, div { color: #ffffff; }

.app-container { display: flex; flex-direction: column; height: 100vh; width: 100%; }
.dashboard-layout { display: flex; flex: 1; height: 65vh; width: 100%; }
.sidebar { flex: 0 0 22%; max-width: 300px; background-color: #121721; border-right: 1px solid #2d3545; padding: 20px; overflow-y: auto; }
.map-container { flex: 1; position: relative; display: flex; flex-direction: column; }
#map { flex: 1; width: 100%; background: #000; z-index: 1; }
.leaflet-popup-content-wrapper, .leaflet-popup-content-wrapper *, .wind-hover-tooltip { color: #000000 !important; font-weight: bold; }
.telemetry-panel { flex: 0 0 22%; max-width: 300px; background-color: #121721; border-left: 1px solid #2d3545; padding: 20px; overflow-y: auto; }
.incident-logs { display: flex; height: 35vh; background-color: #0b0e14; border-top: 1px solid #2d3545; }
.log-column { flex: 1; padding: 20px; overflow-y: auto; border-right: 1px solid #2d3545; }
.log-column:last-child { border-right: none; }
.cards-container { display: flex; flex-direction: column; gap: 10px; margin-top: 15px; }
.custom-select { width: 100%; padding: 10px; margin-top: 10px; background-color: #1e2636; color: #ffffff !important; border: 1px solid #2d3545; border-radius: 5px; cursor: pointer; }
.action-btn { width: 100%; padding: 10px; margin-top: 10px; background-color: #1e2636; color: #ffffff !important; border: 1px solid #2d3545; border-radius: 5px; cursor: pointer; font-weight: bold; }
.action-btn:hover { background-color: #2d3545; }
.sync-btn { background-color: #004080; border-color: #0066cc; }
.sync-btn:hover { background-color: #0059b3; }
.divider { height: 1px; background-color: #2d3545; margin: 20px 0; }
.panel-title { font-size: 1.3rem; margin-bottom: 5px; }
.panel-label { font-size: 0.9rem; color: #d0d7de; }
.risk-scale-text { font-size: 0.9rem; font-weight: bold; margin: 4px 0; }
.helper-text { font-size: 12px; color: #8b949e; margin-top: 20px; }
.summary-card { background-color: #1e2636; border: 1px solid #2d3545; border-radius: 8px; padding: 15px; margin-bottom: 15px; }
.card-subtitle { color: #8b949e; font-size: 13px; font-weight: bold; margin-bottom: 5px; }
.card-value { font-size: 2rem; margin: 0; }
.weather-box { background-color: #1e2636; padding: 15px; border-radius: 8px; margin-bottom: 15px; margin-top: 15px; }
.weather-title { color: #00d2ff; font-weight: bold; margin-bottom: 8px; }
.weather-box ul { list-style: none; padding-left: 0; }
.weather-box li { margin-bottom: 5px; color: #d0d7de; }
.fire-confirmed-card { background-color: #3b1219; border: 1px solid #ff0033; border-radius: 8px; padding: 12px; }
.fire-warning-card { background-color: #161b26; border: 1px solid #ffcc00; border-radius: 8px; padding: 12px; }
.live-alert-overlay { position: absolute; top: 20px; left: 50%; transform: translateX(-50%); background-color: rgba(255, 0, 51, 0.9); padding: 15px 30px; border-radius: 5px; z-index: 1000; box-shadow: 0 0 15px rgba(255, 0, 51, 0.5); font-weight: bold; }

.modal-overlay { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; background-color: rgba(0, 0, 0, 0.7); display: flex; justify-content: center; align-items: center; z-index: 2000; }
.modal-content { background-color: #121721; border: 1px solid #2d3545; border-radius: 10px; padding: 30px; width: 400px; max-width: 90%; position: relative; box-shadow: 0 5px 20px rgba(0,0,0,0.5); }
.close-btn { position: absolute; top: 15px; right: 15px; background: none; border: none; color: #8b949e; font-size: 20px; cursor: pointer; }
.close-btn:hover { color: white; }
.custom-input { width: 100%; padding: 12px; margin-bottom: 15px; background-color: #0b0e14; color: white; border: 1px solid #2d3545; border-radius: 5px; }
.custom-input:focus { outline: none; border-color: #00d2ff; }
</style>