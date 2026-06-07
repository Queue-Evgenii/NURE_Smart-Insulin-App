<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-menu-button />
        </ion-buttons>
        <ion-title>{{ t('forecast.title') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding" :fullscreen="true">
      <div class="page-wrapper">
        <ion-card>
          <ion-card-header>
            <ion-card-title>{{ t('forecast.subtitle') }}</ion-card-title>
            <ion-card-subtitle>{{ t('forecast.description') }}</ion-card-subtitle>
          </ion-card-header>
          <ion-card-content>
            <div v-if="loading" class="ion-text-center ion-padding">
              <ion-spinner name="crescent" />
              <p>{{ t('forecast.loading') }}</p>
            </div>

            <div v-else-if="forecast">
              <!-- Current glucose -->
              <div class="glucose-display">
                <div class="current-glucose">
                  {{ forecast.currentGlucose.toFixed(1) }} <span class="unit">mmol/L</span>
                </div>
                <div class="trend-indicator">
                  <span v-if="forecast.trendPerMinute > 0" class="rising">
                    ↑ {{ (forecast.trendPerMinute * 60).toFixed(1) }} per hour
                  </span>
                  <span v-else-if="forecast.trendPerMinute < 0" class="falling">
                    ↓ {{ Math.abs(forecast.trendPerMinute * 60).toFixed(1) }} per hour
                  </span>
                  <span v-else class="stable">
                    → {{ t('forecast.stable') }}
                  </span>
                </div>
              </div>

              <!-- Risk flags -->
              <div v-if="forecast.riskFlags.length > 0" class="risk-flags">
                <ion-chip
                  v-for="flag in forecast.riskFlags"
                  :key="flag"
                  :color="flagColor(flag)"
                  outline
                >
                  {{ t(`forecast.risks.${flag}`, flag) }}
                </ion-chip>
              </div>

              <!-- Forecast chart (text-based table) -->
              <ion-list v-if="forecast.points.length > 0" lines="full">
                <ion-list-header>
                  <ion-label>{{ t('forecast.table.time') }}</ion-label>
                  <ion-label>{{ t('forecast.table.predicted') }}</ion-label>
                  <ion-label>{{ t('forecast.table.range') }}</ion-label>
                </ion-list-header>
                <ion-item v-for="point in forecast.points" :key="point.minutesAhead">
                  <ion-label>
                    <h3>{{ t('forecast.table.minutesAhead', { m: point.minutesAhead }) }}</h3>
                  </ion-label>
                  <ion-label class="predicted-value">
                    <h3 :class="glucoseClass(point.predicted)">
                      {{ point.predicted.toFixed(1) }}
                    </h3>
                  </ion-label>
                  <ion-label class="range-value">
                    <h3 :class="glucoseClass(point.predicted)">
                      {{ point.lower.toFixed(1) }}–{{ point.upper.toFixed(1) }}
                    </h3>
                  </ion-label>
                </ion-item>
              </ion-list>

              <!-- Data info -->
              <div class="forecast-info">
                <ion-note color="medium">
                  {{ t('forecast.readingsUsed', { n: forecast.readingsUsed }) }}
                </ion-note>
                <ion-note v-if="forecast.riskFlags.includes('LOW_CONFIDENCE')" color="warning">
                  {{ t('forecast.lowConfidenceWarning') }}
                </ion-note>
              </div>
            </div>

            <div v-else class="empty-state">
              <p>{{ t('forecast.noData') }}</p>
              <ion-button expand="block" fill="outline" @click="refresh">
                {{ t('common.refresh') }}
              </ion-button>
            </div>

            <ion-button
              v-if="!loading"
              expand="block"
              @click="refresh"
              class="ion-margin-top"
              fill="outline"
            >
              {{ t('common.refresh') }}
            </ion-button>
          </ion-card-content>
        </ion-card>

        <ion-card class="info-card">
          <ion-card-header>
            <ion-card-title>{{ t('forecast.howItWorks') }}</ion-card-title>
          </ion-card-header>
          <ion-card-content>
            <ul>
              <li>{{ t('forecast.info.trend') }}</li>
              <li>{{ t('forecast.info.insulin') }}</li>
              <li>{{ t('forecast.info.carbs') }}</li>
              <li>{{ t('forecast.info.band') }}</li>
            </ul>
          </ion-card-content>
        </ion-card>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  IonPage, IonHeader, IonToolbar, IonButtons, IonMenuButton, IonTitle, IonContent,
  IonCard, IonCardHeader, IonCardTitle, IonCardSubtitle, IonCardContent,
  IonList, IonListHeader, IonItem, IonLabel, IonButton, IonNote, IonSpinner, IonChip,
  toastController,
} from '@ionic/vue';
import { apiFetch } from '@/services/api';

const { t } = useI18n();

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

const forecast = ref<Forecast | null>(null);
const loading = ref(false);

function glucoseClass(value: number): string {
  if (value < 3.9) return 'hypo';
  if (value > 10.0) return 'hyper';
  return 'normal';
}

function flagColor(flag: string): string {
  if (['HYPO_RISK'].includes(flag)) return 'danger';
  if (['HYPER_RISK'].includes(flag)) return 'warning';
  if (['LOW_CONFIDENCE'].includes(flag)) return 'medium';
  return 'primary';
}

async function loadForecast() {
  loading.value = true;
  forecast.value = null;
  try {
    const res = await apiFetch('/api/forecast', {
      method: 'POST',
      body: JSON.stringify({ horizonMinutes: 180 }),
    });
    if (res.ok) {
      forecast.value = await res.json();
    } else {
      const toast = await toastController.create({
        message: t('forecast.error'),
        duration: 2000,
        color: 'danger',
      });
      await toast.present();
    }
  } finally {
    loading.value = false;
  }
}

function refresh() {
  loadForecast();
}

onMounted(() => loadForecast());
</script>

<style scoped>
.page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.glucose-display {
  text-align: center;
  padding: 16px 0;
  border-bottom: 1px solid var(--ion-color-light);
}

.current-glucose {
  font-size: 48px;
  font-weight: 700;
  color: var(--ion-color-primary);
  line-height: 1.1;
}

.unit {
  font-size: 18px;
  font-weight: 400;
  color: var(--ion-color-medium);
  margin-left: 4px;
}

.trend-indicator {
  font-size: 14px;
  margin-top: 8px;
  font-weight: 500;
}

.rising {
  color: var(--ion-color-danger);
}

.falling {
  color: var(--ion-color-success);
}

.stable {
  color: var(--ion-color-medium);
}

.risk-flags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 12px 0;
}

ion-list-header {
  display: flex;
  padding: 8px 16px;
  border-bottom: 2px solid var(--ion-color-light);
  font-weight: 600;
  font-size: 12px;
}

ion-list-header ion-label {
  flex: 1;
}

ion-list-header ion-label:nth-child(2) {
  text-align: center;
}

ion-list-header ion-label:nth-child(3) {
  text-align: right;
}

ion-item {
  display: flex;
  align-items: center;
}

ion-item ion-label {
  flex: 1;
}

.predicted-value {
  text-align: center;
  flex: 0.8;
}

.range-value {
  text-align: right;
  flex: 1;
}

.hypo {
  color: var(--ion-color-danger);
  font-weight: 600;
}

.hyper {
  color: var(--ion-color-warning);
  font-weight: 600;
}

.normal {
  color: var(--ion-color-success);
  font-weight: 600;
}

.forecast-info {
  margin-top: 12px;
  padding: 8px 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.empty-state {
  text-align: center;
  padding: 24px 0;
  color: var(--ion-color-medium);
}

.info-card {
  margin-bottom: 16px;
}

.info-card ul {
  padding-left: 20px;
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--ion-color-medium);
}

.info-card li {
  margin-bottom: 6px;
}
</style>
