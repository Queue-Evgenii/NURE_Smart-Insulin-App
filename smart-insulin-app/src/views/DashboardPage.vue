<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-menu-button />
        </ion-buttons>
        <ion-title>{{ t('dashboard.title') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content :fullscreen="true">
      <div class="dashboard-wrapper">
        <!-- Block 1: Forecast -->
        <div class="dashboard-block">
          <ion-card v-if="forecast" class="forecast-card">
            <ion-card-header>
              <ion-card-title class="block-title">{{ t('dashboard.forecast.title') }}</ion-card-title>
              <ion-card-subtitle>{{ t('dashboard.forecast.subtitle') }}</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content>
              <div v-if="forecastLoading" class="ion-text-center">
                <ion-spinner name="crescent" />
              </div>
              <div v-else class="forecast-preview">
                <div class="forecast-header">
                  <div class="forecast-current">
                    <span class="glucose-value">{{ forecast.currentGlucose.toFixed(1) }}</span>
                    <span class="trend" :class="trendClass">{{ trendText }}</span>
                  </div>
                  <div v-if="forecast.riskFlags.length > 0" class="risk-indicator">
                    <ion-badge
                      v-for="flag in forecast.riskFlags"
                      :key="flag"
                      :color="riskColor(flag)"
                    >
                      {{ t(`dashboard.forecast.risks.${flag}`, flag) }}
                    </ion-badge>
                  </div>
                </div>
                <div class="forecast-chart-container">
                  <Line :data="forecastChartData" :options="forecastChartOptions" />
                </div>
                <div class="forecast-legend">
                  {{ t('forecast.readingsUsed', { n: forecast.readingsUsed }) }}
                </div>
              </div>
            </ion-card-content>
          </ion-card>
        </div>

        <!-- Block 2 & 3: Assessment and Input (2-column on wide, stacked on mobile) -->
        <div class="dashboard-row">
          <!-- Block 2: HbA1c Assessment -->
          <div class="dashboard-block">
            <ion-card style="height: 100%;">
              <ion-card-header>
                <ion-card-title class="block-title">{{ t('dashboard.hba1c.title') }}</ion-card-title>
                <ion-card-subtitle>{{ t('dashboard.hba1c.subtitle') }}</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content>
                <div v-if="hba1cLoading" class="ion-text-center">
                  <ion-spinner name="crescent" />
                </div>
                <div v-else-if="hba1c" class="hba1c-display">
                  <div class="hba1c-value" :class="hba1cColor">{{ hba1c.hba1c.toFixed(1) }}%</div>
                  <div class="hba1c-details">
                    <span>{{ t('dashboard.hba1c.avgGlucose') }} {{ hba1c.averageGlucose.toFixed(1) }} {{ t('common.unitGlucose') }}</span>
                    <span>{{ hba1c.readingsCount }} {{ t('dashboard.hba1c.readings') }}</span>
                  </div>
                </div>
                <div v-else class="ion-text-center">
                  <p>{{ t('dashboard.hba1c.noReadings') }}</p>
                </div>
              </ion-card-content>
            </ion-card>
          </div>

          <!-- Block 3: Add Reading -->
          <div class="dashboard-block">
            <ion-card>
              <ion-card-header>
                <ion-card-title class="block-title">{{ t('dashboard.addReading.title') }}</ion-card-title>
              </ion-card-header>
              <ion-card-content>
                <ion-list lines="none">
                  <ion-item>
                    <ion-input
                      v-model.number="newGlucose"
                      type="number"
                      :label="t('dashboard.addReading.glucose')"
                      label-placement="floating"
                      placeholder="5.5"
                      step="0.1"
                      min="0"
                    />
                  </ion-item>
                  <ion-item>
                    <ion-select
                      v-model="newMeasurementType"
                      :label="t('dashboard.addReading.measurementType')"
                      label-placement="floating"
                      interface="action-sheet"
                    >
                      <ion-select-option value="MANUAL">{{ t('dashboard.addReading.manual') }}</ion-select-option>
                      <ion-select-option value="CGM">{{ t('dashboard.addReading.cgm') }}</ion-select-option>
                    </ion-select>
                  </ion-item>
                  <ion-item>
                    <ion-input
                      v-model="newNotes"
                      type="text"
                      :label="t('common.notes')"
                      label-placement="floating"
                    />
                  </ion-item>
                </ion-list>
                <ion-button expand="block" :disabled="addingReading || !newGlucose" @click="addReading" class="ion-margin-top">
                  <ion-spinner v-if="addingReading" name="crescent" />
                  <span v-else>{{ t('dashboard.addReading.button') }}</span>
                </ion-button>
              </ion-card-content>
            </ion-card>
          </div>
        </div>

        <!-- Block 4: History with infinite scroll -->
        <div class="dashboard-block">
          <ion-card class="readings-card">
            <ion-card-header>
              <ion-card-title class="block-title">{{ t('dashboard.recentReadings.title') }}</ion-card-title>
            </ion-card-header>
            <ion-card-content>
              <div v-if="readingsLoading && readings.length === 0" class="ion-text-center">
                <ion-spinner name="crescent" />
              </div>
              <ion-list v-else-if="readings.length > 0" lines="full">
                <ion-item-sliding v-for="r in readings" :key="r.id">
                  <ion-item>
                    <ion-label>
                      <h2 :class="glucoseColor(r.glucoseValue)">{{ r.glucoseValue.toFixed(1) }} {{ t('common.unitGlucose') }}</h2>
                      <p>{{ formatDate(r.measuredAt) }} · {{ r.measurementType }}</p>
                      <p v-if="r.notes" class="notes">{{ r.notes }}</p>
                    </ion-label>
                  </ion-item>
                  <ion-item-options side="end">
                    <ion-item-option color="danger" @click="deleteReading(r.id)">
                      <ion-icon slot="icon-only" :icon="trashOutline" />
                    </ion-item-option>
                  </ion-item-options>
                </ion-item-sliding>
              </ion-list>
              <div v-else class="ion-text-center">
                <p>{{ t('dashboard.recentReadings.empty') }}</p>
              </div>

              <ion-button v-if="hasMore" expand="block" fill="clear" @click="loadMore" :disabled="readingsLoading">
                <ion-spinner v-if="readingsLoading" name="crescent" />
                <span v-else>{{ t('common.loadMore') }}</span>
              </ion-button>
            </ion-card-content>
          </ion-card>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip, Legend } from 'chart.js';
import { Line } from 'vue-chartjs';
import {
  IonPage, IonHeader, IonToolbar, IonButtons, IonMenuButton, IonTitle, IonContent,
  IonCard, IonCardHeader, IonCardTitle, IonCardSubtitle, IonCardContent,
  IonList, IonItem, IonItemSliding, IonItemOptions, IonItemOption,
  IonLabel, IonInput, IonSelect, IonSelectOption, IonButton, IonIcon, IonSpinner, IonBadge,
  toastController,
} from '@ionic/vue';
import { trashOutline } from 'ionicons/icons';
import { apiFetch } from '@/services/api';

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip, Legend);

const { t } = useI18n();

interface GlucoseReading {
  id: number;
  glucoseValue: number;
  measurementType: string;
  measuredAt: string;
  notes: string | null;
  createdAt: string;
}

interface HbA1cData {
  hba1c: number;
  readingsCount: number;
  averageGlucose: number;
  from: string;
  to: string;
}

interface ForecastPoint {
  minutesAhead: number;
  predicted: number;
  lower: number;
  upper: number;
}

interface Forecast {
  currentGlucose: number;
  trendPerMinute: number;
  points: ForecastPoint[];
  riskFlags: string[];
  readingsUsed: number;
}

const readings = ref<GlucoseReading[]>([]);
const readingsLoading = ref(false);
const currentPage = ref(0);
const hasMore = ref(true);

const hba1c = ref<HbA1cData | null>(null);
const hba1cLoading = ref(false);
const hba1cColor = ref('');

const forecast = ref<Forecast | null>(null);
const forecastLoading = ref(false);

const newGlucose = ref<number | null>(null);
const newMeasurementType = ref('MANUAL');
const newNotes = ref('');
const addingReading = ref(false);

function glucoseColor(value: number): string {
  if (value < 4.0) return 'glucose-low';
  if (value > 10.0) return 'glucose-high';
  return 'glucose-normal';
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString();
}

const trendClass = computed(() => {
  if (!forecast.value) return '';
  if (forecast.value.trendPerMinute > 0) return 'rising';
  if (forecast.value.trendPerMinute < 0) return 'falling';
  return 'stable';
});

const trendText = computed(() => {
  if (!forecast.value) return '';
  const per60 = forecast.value.trendPerMinute * 60;
  if (per60 > 0) return `↑ ${per60.toFixed(1)}`;
  if (per60 < 0) return `↓ ${Math.abs(per60).toFixed(1)}`;
  return '→';
});

const forecastChartData = computed(() => {
  if (!forecast.value) return { labels: [], datasets: [] };

  const labels = forecast.value.points.map(p => `${p.minutesAhead}'`);
  const predicted = forecast.value.points.map(p => p.predicted);
  const lower = forecast.value.points.map(p => p.lower);
  const upper = forecast.value.points.map(p => p.upper);

  return {
    labels,
    datasets: [
      {
        label: t('forecast.predicted'),
        data: predicted,
        borderColor: '#3880ff',
        backgroundColor: 'rgba(56, 128, 255, 0.1)',
        borderWidth: 2,
        fill: false,
        tension: 0.4,
        pointRadius: 4,
        pointBackgroundColor: '#3880ff',
      },
      {
        label: t('forecast.band'),
        data: upper,
        borderColor: 'transparent',
        backgroundColor: 'rgba(56, 128, 255, 0.05)',
        fill: '-1',
        borderWidth: 0,
        pointRadius: 0,
      },
      {
        label: t('forecast.lowerBand'),
        data: lower,
        borderColor: 'transparent',
        backgroundColor: 'transparent',
        borderWidth: 0,
        pointRadius: 0,
      },
    ],
  };
});

const forecastChartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false,
    },
    tooltip: {
      callbacks: {
        label: (context: any) => {
          if (context.datasetIndex === 0) {
            return `${t('forecast.predicted')}: ${context.parsed.y.toFixed(1)}`;
          }
          return '';
        },
      },
    },
  },
  scales: {
    y: {
      min: 2,
      max: 12,
      grid: {
        color: 'rgba(255, 255, 255, 0.1)',
      },
      ticks: {
        color: 'rgba(255, 255, 255, 0.7)',
      },
    },
    x: {
      grid: {
        display: false,
      },
      ticks: {
        color: 'rgba(255, 255, 255, 0.7)',
      },
    },
  },
}));


function riskColor(flag: string): string {
  if (flag === 'HYPO_RISK') return 'danger';
  if (flag === 'HYPER_RISK') return 'warning';
  return 'medium';
}

async function loadForecast() {
  forecastLoading.value = true;
  try {
    const res = await apiFetch('/api/forecast', {
      method: 'POST',
      body: JSON.stringify({ horizonMinutes: 180 }),
    });
    if (res.ok) {
      forecast.value = await res.json();
    }
  } finally {
    forecastLoading.value = false;
  }
}

async function loadReadings(page = 0) {
  readingsLoading.value = true;
  try {
    const res = await apiFetch(`/api/glucose?page=${page}&size=20`);
    if (res.ok) {
      const data = await res.json();
      if (page === 0) readings.value = data.content;
      else readings.value.push(...data.content);
      hasMore.value = !data.last;
      currentPage.value = page;
    }
  } finally {
    readingsLoading.value = false;
  }
}

function loadMore() { loadReadings(currentPage.value + 1); }

async function loadHbA1c() {
  hba1cLoading.value = true;
  try {
    const to = new Date().toISOString();
    const from = new Date(Date.now() - 90 * 24 * 60 * 60 * 1000).toISOString();
    const res = await apiFetch('/api/glucose/hba1c', { method: 'POST', body: JSON.stringify({ from, to }) });
    if (res.ok) {
      const data: HbA1cData = await res.json();
      hba1c.value = data.readingsCount > 0 ? data : null;
      if (data.hba1c < 7) hba1cColor.value = 'hba1c-good';
      else if (data.hba1c < 8) hba1cColor.value = 'hba1c-moderate';
      else hba1cColor.value = 'hba1c-high';
    }
  } finally {
    hba1cLoading.value = false;
  }
}

async function addReading() {
  if (!newGlucose.value || newGlucose.value <= 0) return;
  addingReading.value = true;
  try {
    const res = await apiFetch('/api/glucose', {
      method: 'POST',
      body: JSON.stringify({
        glucoseValue: newGlucose.value,
        measurementType: newMeasurementType.value,
        measuredAt: new Date().toISOString(),
        notes: newNotes.value || null,
      }),
    });
    if (res.ok) {
      newGlucose.value = null;
      newNotes.value = '';
      await Promise.all([loadReadings(0), loadHbA1c(), loadForecast()]);
      const toast = await toastController.create({ message: t('dashboard.addReading.success'), duration: 1500 });
      await toast.present();
    } else {
      const toast = await toastController.create({ message: t('dashboard.addReading.error'), duration: 2000, color: 'danger' });
      await toast.present();
    }
  } finally {
    addingReading.value = false;
  }
}

async function deleteReading(id: number) {
  const res = await apiFetch(`/api/glucose/${id}`, { method: 'DELETE' });
  if (res.ok) {
    readings.value = readings.value.filter(r => r.id !== id);
    loadHbA1c();
  }
}

onMounted(() => { loadReadings(0); loadHbA1c(); loadForecast(); });
</script>

<style scoped>
/* Layout structure */
.dashboard-wrapper {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dashboard-block {
  width: 100%;
}

.dashboard-row {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

ion-card {
  margin: 0;
  --background: rgba(255, 255, 255, 0.08) !important;
  background: rgba(255, 255, 255, 0.08) !important;
}

ion-item {
  --background: transparent;
}

/* Adaptive titles */
.block-title {
  font-size: clamp(16px, 5vw, 24px);
  font-weight: 600;
}

/* HbA1c section */
.hba1c-display {
  text-align: center;
}

.hba1c-value {
  font-size: clamp(36px, 12vw, 56px);
  font-weight: 700;
  line-height: 1.2;
}

.hba1c-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 12px;
  color: var(--ion-color-medium);
  font-size: clamp(12px, 3vw, 14px);
}

.hba1c-good {
  color: var(--ion-color-success);
}

.hba1c-moderate {
  color: var(--ion-color-warning);
}

.hba1c-high {
  color: var(--ion-color-danger);
}

/* Glucose colors */
.glucose-low {
  color: var(--ion-color-warning);
}

.glucose-normal {
  color: var(--ion-color-success);
}

.glucose-high {
  color: var(--ion-color-danger);
}

.notes {
  font-style: italic;
  color: var(--ion-color-medium);
}

/* Forecast section */
.forecast-preview {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.forecast-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.forecast-current {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 8px;
}

.glucose-value {
  font-size: clamp(28px, 8vw, 40px);
  font-weight: 700;
  color: var(--ion-color-primary);
}

.trend {
  font-size: clamp(12px, 3vw, 14px);
  font-weight: 500;
}

.trend.rising {
  color: var(--ion-color-danger);
}

.trend.falling {
  color: var(--ion-color-success);
}

.trend.stable {
  color: var(--ion-color-medium);
}

.risk-indicator {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  justify-content: center;
}

.forecast-chart-container {
  height: 280px;
  position: relative;
  width: 100%;
}

.forecast-legend {
  font-size: 12px;
  color: var(--ion-color-medium);
  text-align: center;
}

.readings-card {
  height: auto;
}

/* Tablet and desktop layout */
@media (min-width: 768px) {
  .dashboard-wrapper {
    padding: 24px;
  }

  .dashboard-row {
    flex-direction: row;
    gap: 24px;
  }

  .dashboard-block {
    flex: 1;
  }

  .dashboard-row .dashboard-block:first-child {
    flex: 0 0 calc(50% - 12px);
  }

  .dashboard-row .dashboard-block:last-child {
    flex: 0 0 calc(50% - 12px);
  }
}

@media (min-width: 1024px) {
  .dashboard-wrapper {
    padding: 32px;
    gap: 24px;
  }

  .dashboard-row {
    gap: 24px;
  }
}
</style>
