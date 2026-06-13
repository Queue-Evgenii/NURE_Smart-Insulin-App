<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-menu-button />
        </ion-buttons>
        <ion-title>{{ t('bolus.title') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding" :fullscreen="true">
      <div class="page-wrapper">
        <div class="bolus-layout">

          <!-- Left column: calculator -->
          <div class="bolus-left">

          <ion-card>
            <ion-card-header>
              <ion-card-title>{{ t('bolus.form.title') }}</ion-card-title>
              <ion-card-subtitle>{{ t('bolus.form.subtitle') }}</ion-card-subtitle>
            </ion-card-header>
            <ion-card-content>
              <ion-list lines="none">
                <ion-item>
                  <ion-input
                    v-model.number="currentGlucose"
                    type="number"
                    :label="t('bolus.form.glucose')"
                    label-placement="floating"
                    placeholder="5.5"
                    step="0.1"
                    min="0"
                  />
                </ion-item>
                <ion-item>
                  <ion-input
                    v-model="mealName"
                    type="text"
                    :label="t('bolus.form.mealName')"
                    label-placement="floating"
                    :placeholder="t('bolus.form.mealNamePlaceholder')"
                  />
                </ion-item>

                <!-- AI carbs estimation -->
                <ion-item lines="none" class="ai-estimate-row">
                  <ion-button
                    slot="end"
                    size="small"
                    fill="outline"
                    :disabled="estimatingCarbs || !mealName.trim()"
                    @click="estimateCarbs"
                  >
                    <ion-spinner v-if="estimatingCarbs" name="crescent" slot="start" />
                    <ion-icon v-else :icon="sparkles" slot="start" />
                    {{ t('bolus.form.estimateCarbs') }}
                  </ion-button>
                </ion-item>
                <ion-note v-if="carbsBreakdown" color="medium" class="breakdown-note">
                  {{ carbsBreakdown }}
                </ion-note>

                <ion-item>
                  <ion-input
                    v-model.number="carbsG"
                    type="number"
                    :label="t('bolus.form.carbs')"
                    label-placement="floating"
                    placeholder="60"
                    step="1"
                    min="0"
                  />
                </ion-item>
                <ion-item>
                  <ion-input
                    v-model.number="glycemicIndex"
                    type="number"
                    :label="t('bolus.form.gi')"
                    label-placement="floating"
                    placeholder="55"
                    step="1"
                    min="1"
                    max="100"
                  />
                </ion-item>

                <!-- Activity adjustment (section 2.1.2) -->
                <ion-item>
                  <ion-select
                    v-model="activityType"
                    :label="t('bolus.form.activityType')"
                    label-placement="floating"
                    placeholder="—"
                    :interface-options="{ mode: 'ios' }"
                  >
                    <ion-select-option value="">{{ t('common.none') }}</ion-select-option>
                    <ion-select-option value="AEROBIC">{{ t('bolus.form.activityAerobic') }}</ion-select-option>
                    <ion-select-option value="ANAEROBIC">{{ t('bolus.form.activityAnaerobic') }}</ion-select-option>
                    <ion-select-option value="MIXED">{{ t('bolus.form.activityMixed') }}</ion-select-option>
                  </ion-select>
                </ion-item>
                <ion-item v-if="activityType">
                  <ion-select
                    v-model="activityIntensity"
                    :label="t('bolus.form.activityIntensity')"
                    label-placement="floating"
                    placeholder="—"
                    :interface-options="{ mode: 'ios' }"
                  >
                    <ion-select-option value="LIGHT">{{ t('bolus.form.activityLight') }}</ion-select-option>
                    <ion-select-option value="MODERATE">{{ t('bolus.form.activityModerate') }}</ion-select-option>
                    <ion-select-option value="HIGH">{{ t('bolus.form.activityHigh') }}</ion-select-option>
                    <ion-select-option value="MAXIMAL">{{ t('bolus.form.activityMaximal') }}</ion-select-option>
                  </ion-select>
                </ion-item>
                <ion-item v-if="activityType">
                  <ion-input
                    v-model.number="minutesUntilActivity"
                    type="number"
                    :label="t('bolus.form.activityMinutes')"
                    label-placement="floating"
                    placeholder="0"
                    step="5"
                    min="0"
                    max="480"
                  />
                </ion-item>
                <ion-item v-if="activityType">
                  <ion-input
                    v-model.number="activityDurationMinutes"
                    type="number"
                    :label="t('bolus.form.activityDuration')"
                    label-placement="floating"
                    placeholder="30"
                    step="5"
                    min="5"
                    max="300"
                  />
                </ion-item>
              </ion-list>

              <ion-button
                expand="block"
                :disabled="calculating || !currentGlucose || carbsG === null"
                @click="calculate"
                class="ion-margin-top"
              >
                <ion-spinner v-if="calculating" name="crescent" />
                <span v-else>{{ t('bolus.form.calculate') }}</span>
              </ion-button>
            </ion-card-content>
          </ion-card>

          <!-- Result card -->
          <ion-card v-if="result" class="result-card">
            <ion-card-header>
              <ion-card-title>{{ t('bolus.result.title') }}</ion-card-title>
            </ion-card-header>
            <ion-card-content>
              <div class="dose-result">
                <div class="dose-total" :class="result.activityFactor ? 'adjusted' : ''">
                  {{ (result.adjustedDose || result.totalDose).toFixed(1) }} {{ t('common.unitInsulin') }}
                </div>
                <div v-if="result.activityFactor" class="activity-factors">
                  <small>{{ t('bolus.result.baselineDose', { dose: result.totalDose.toFixed(1) }) }}</small>
                  <small>{{ t('bolus.result.activityFactor', { f: (result.activityFactor * 100).toFixed(0), t: (result.timeFactor! * 100).toFixed(0), d: (result.durationFactor! * 100).toFixed(0) }) }}</small>
                </div>
                <div class="dose-breakdown">
                  <div class="breakdown-row">
                    <span>{{ t('bolus.result.carbDose') }}</span>
                    <span>{{ result.bolusForCarbs.toFixed(1) }} {{ t('common.unitInsulin') }}</span>
                  </div>
                  <div class="breakdown-row">
                    <span>{{ t('bolus.result.correction') }}</span>
                    <span :class="result.correctionDose >= 0 ? 'positive' : 'negative'">
                      {{ result.correctionDose >= 0 ? '+' : '' }}{{ result.correctionDose.toFixed(1) }} {{ t('common.unitInsulin') }}
                    </span>
                  </div>
                  <div class="breakdown-row iob-row">
                    <span>{{ t('bolus.result.iob') }}</span>
                    <span class="negative">−{{ result.currentIob.toFixed(1) }} {{ t('common.unitInsulin') }}</span>
                  </div>
                </div>

                <ion-note v-if="timingHint" :color="timingColor" class="timing-note">
                  {{ timingHint }}
                </ion-note>
              </div>

              <ion-note v-if="result.missingParams.length > 0" color="warning" class="missing-note">
                {{ t('bolus.result.missingParams', { params: result.missingParams.join(', ') }) }}
              </ion-note>
              <ion-note v-if="result.activityWarning" color="warning" class="activity-warning-note">
                ⚠️ {{ result.activityWarning }}
              </ion-note>

              <ion-list lines="none" class="ion-margin-top">
                <ion-item>
                  <ion-input
                    v-model.number="adjustedDose"
                    type="number"
                    :label="`${t('bolus.result.doseInput')} (${t('common.unitInsulin')})`"
                    label-placement="floating"
                    step="0.5"
                    min="0"
                    :helper-text="t('bolus.result.recommended', { dose: result.totalDose.toFixed(1) })"
                  />
                </ion-item>
              </ion-list>

              <ion-button
                expand="block"
                class="ion-margin-top"
                @click="logDose"
                :disabled="loggingDose || !adjustedDose"
              >
                <ion-spinner v-if="loggingDose" name="crescent" />
                <span v-else>{{ t('bolus.result.logButton') }}</span>
              </ion-button>
            </ion-card-content>
          </ion-card>

          <!-- AI Recommendation card -->
          <ion-card v-if="result" class="recommendation-card">
            <ion-card-header>
              <ion-card-title>
                <ion-icon :icon="sparkles" class="ai-icon" />
                {{ t('bolus.recommendation.title') }}
              </ion-card-title>
            </ion-card-header>
            <ion-card-content>
              <div v-if="!recommendation && !loadingRecommendation" class="rec-placeholder">
                <ion-button expand="block" fill="outline" @click="fetchRecommendation">
                  {{ t('bolus.recommendation.getButton') }}
                </ion-button>
              </div>
              <div v-else-if="loadingRecommendation" class="ion-text-center ion-padding">
                <ion-spinner name="crescent" />
                <p>{{ t('bolus.recommendation.loading') }}</p>
              </div>
              <div v-else-if="recommendation">
                <ion-note
                  v-for="flag in recommendation.safetyFlags"
                  :key="flag"
                  :color="flagColor(flag)"
                  class="safety-flag"
                >
                  {{ t(`bolus.recommendation.flags.${flag}`, flag) }}
                </ion-note>
                <p class="rec-message">{{ recommendation.message }}</p>
              </div>
            </ion-card-content>
          </ion-card>

          </div><!-- /bolus-left -->

          <!-- Right column: recent doses -->
          <ion-card class="doses-card">
            <ion-card-header>
              <ion-card-title>{{ t('bolus.recentDoses.title') }}</ion-card-title>
            </ion-card-header>
            <ion-card-content>
              <div v-if="dosesLoading" class="ion-text-center">
                <ion-spinner name="crescent" />
              </div>
              <ion-list v-else-if="recentDoses.length > 0" lines="full">
                <ion-item v-for="d in recentDoses" :key="d.id">
                  <ion-label>
                    <h2>
                      <span class="dose-units">{{ d.doseUnits }} {{ t('common.unitInsulin') }}</span>
                      &nbsp;
                      <ion-badge :color="doseTypeColor(d.doseType)">{{ t(`doses.add.${d.doseType.toLowerCase()}`) }}</ion-badge>
                    </h2>
                    <p>{{ formatDate(d.injectedAt) }}</p>
                    <p v-if="d.mealName">{{ t('doses.list.meal', { name: d.mealName }) }}</p>
                    <p v-if="d.glucoseBefore != null">{{ t('doses.list.glucoseBefore', { value: d.glucoseBefore }) }}</p>
                    <p v-if="d.notes" class="dose-notes">{{ d.notes }}</p>
                  </ion-label>
                </ion-item>
              </ion-list>
              <div v-else class="ion-text-center ion-padding">
                <p>{{ t('bolus.recentDoses.empty') }}</p>
              </div>
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
import {
  IonPage, IonHeader, IonToolbar, IonButtons, IonMenuButton, IonTitle, IonContent,
  IonCard, IonCardHeader, IonCardTitle, IonCardSubtitle, IonCardContent,
  IonList, IonItem, IonLabel, IonBadge, IonInput, IonButton,
  IonSpinner, IonNote, IonIcon, IonSelect, IonSelectOption,
  toastController,
} from '@ionic/vue';
import { sparkles } from 'ionicons/icons';
import { apiFetch } from '@/services/api';

const { t } = useI18n();

interface BolusResult {
  bolusForCarbs: number;
  correctionDose: number;
  currentIob: number;
  totalDose: number;
  mealRecordId: number | null;
  missingParams: string[];
  adjustedDose: number | null;
  usingAdaptiveCoefficients: boolean;
  activityFactor: number | null;
  timeFactor: number | null;
  durationFactor: number | null;
  activityWarning: string | null;
}

interface InsulinDose {
  id: number;
  doseUnits: number;
  doseType: string;
  injectedAt: string;
  mealName: string | null;
  glucoseBefore: number | null;
  notes: string | null;
}

const currentGlucose = ref<number | null>(null);
const mealName = ref('');
const carbsG = ref<number | null>(null);
const glycemicIndex = ref<number | null>(null);
const calculating = ref(false);
const loggingDose = ref(false);
const estimatingCarbs = ref(false);
const carbsBreakdown = ref('');

// Activity adjustment (section 2.1.2)
const activityType = ref('');  // AEROBIC | ANAEROBIC | MIXED
const activityIntensity = ref('');  // LIGHT | MODERATE | HIGH | MAXIMAL
const minutesUntilActivity = ref(0);
const activityDurationMinutes = ref(0);

interface Recommendation { message: string; safetyFlags: string[] }
const recommendation = ref<Recommendation | null>(null);
const loadingRecommendation = ref(false);
const result = ref<BolusResult | null>(null);
const adjustedDose = ref<number | null>(null);

const timingHint = computed(() => {
  if (!glycemicIndex.value || !result.value) return '';
  if (glycemicIndex.value > 70) return t('bolus.form.timingHigh');
  if (glycemicIndex.value >= 55) return t('bolus.form.timingMedium');
  return t('bolus.form.timingLow');
});

const timingColor = computed(() => {
  if (!glycemicIndex.value) return '';
  if (glycemicIndex.value > 70) return 'danger';
  if (glycemicIndex.value >= 55) return 'warning';
  return 'success';
});

const recentDoses = ref<InsulinDose[]>([]);
const dosesLoading = ref(false);

function formatDate(iso: string): string { return new Date(iso).toLocaleString(); }

function doseTypeColor(type: string): string {
  if (type === 'BOLUS') return 'primary';
  if (type === 'BASAL') return 'secondary';
  return 'warning';
}

async function loadRecentDoses() {
  dosesLoading.value = true;
  try {
    const res = await apiFetch('/api/doses?page=0&size=10');
    if (res.ok) {
      const data = await res.json();
      recentDoses.value = data.content;
    }
  } finally {
    dosesLoading.value = false;
  }
}

function flagColor(flag: string): string {
  if (['HYPOGLYCEMIA', 'LARGE_DOSE'].includes(flag)) return 'danger';
  if (['HIGH_GLUCOSE', 'HIGH_IOB', 'PHYSICAL_ACTIVITY'].includes(flag)) return 'warning';
  return 'medium';
}

async function fetchRecommendation() {
  if (!result.value) return;
  loadingRecommendation.value = true;
  recommendation.value = null;
  try {
    const res = await apiFetch('/api/bolus/recommendation', {
      method: 'POST',
      body: JSON.stringify({
        currentGlucose: currentGlucose.value,
        glucoseTrend: 0,
        bolusForCarbs: result.value.bolusForCarbs,
        correctionDose: result.value.correctionDose,
        currentIob: result.value.currentIob,
        totalDose: result.value.totalDose,
        carbsG: carbsG.value ?? 0,
        plannedActivity: null,
      }),
    });
    if (res.ok) {
      recommendation.value = await res.json();
    }
  } finally {
    loadingRecommendation.value = false;
  }
}

async function estimateCarbs() {
  if (!mealName.value.trim()) return;
  estimatingCarbs.value = true;
  carbsBreakdown.value = '';
  try {
    const res = await apiFetch('/api/bolus/estimate-carbs', {
      method: 'POST',
      body: JSON.stringify({ mealDescription: mealName.value }),
    });
    if (res.ok) {
      const data = await res.json();
      carbsG.value = data.estimatedCarbsG;
      carbsBreakdown.value = data.breakdown;
    } else {
      const toast = await toastController.create({ message: t('bolus.form.estimateError'), duration: 2000, color: 'danger' });
      await toast.present();
    }
  } finally {
    estimatingCarbs.value = false;
  }
}

async function calculate() {
  if (!currentGlucose.value || carbsG.value === null) return;
  calculating.value = true;
  result.value = null;
  recommendation.value = null;
  try {
    const body: any = { currentGlucose: currentGlucose.value, carbsG: carbsG.value };
    if (activityType.value || activityIntensity.value) {
      body.activity = {
        type: activityType.value,
        intensity: activityIntensity.value,
        minutesUntilStart: minutesUntilActivity.value || 0,
        durationMinutes: activityDurationMinutes.value || 0,
      };
    }
    const res = await apiFetch('/api/bolus/calculate', {
      method: 'POST',
      body: JSON.stringify(body),
    });
    if (res.ok) {
      result.value = await res.json();
      adjustedDose.value = result.value!.adjustedDose || result.value!.totalDose;
    } else {
      const toast = await toastController.create({ message: t('bolus.form.errorCalc'), duration: 2000, color: 'danger' });
      await toast.present();
    }
  } finally {
    calculating.value = false;
  }
}

async function logDose() {
  if (!result.value) return;
  loggingDose.value = true;
  try {
    const now = new Date().toISOString();

    // Save meal record first, then link it to the dose
    let mealRecordId: number | null = null;
    const mealRes = await apiFetch('/api/meals', {
      method: 'POST',
      body: JSON.stringify({
        mealName: mealName.value || null,
        carbohydratesG: carbsG.value,
        glycemicIndex: glycemicIndex.value || null,
        mealTime: now,
      }),
    });
    if (mealRes.ok) {
      const meal = await mealRes.json();
      mealRecordId = meal.id;
    }

    await apiFetch('/api/glucose', {
      method: 'POST',
      body: JSON.stringify({
        glucoseValue: currentGlucose.value,
        measurementType: 'MANUAL',
        measuredAt: now,
      }),
    });

    const doseRes = await apiFetch('/api/doses', {
      method: 'POST',
      body: JSON.stringify({
        doseUnits: adjustedDose.value,
        doseType: 'BOLUS',
        injectedAt: now,
        glucoseBefore: currentGlucose.value,
        mealRecordId,
      }),
    });
    if (doseRes.ok) {
      const toast = await toastController.create({ message: t('bolus.result.success'), duration: 1500, color: 'success' });
      await toast.present();
      result.value = null;
      recommendation.value = null;
      adjustedDose.value = null;
      currentGlucose.value = null;
      mealName.value = '';
      carbsBreakdown.value = '';
      carbsG.value = null;
      glycemicIndex.value = null;
      await loadRecentDoses();
    } else {
      const toast = await toastController.create({ message: t('bolus.result.error'), duration: 2000, color: 'danger' });
      await toast.present();
    }
  } finally {
    loggingDose.value = false;
  }
}

onMounted(() => loadRecentDoses());
</script>

<style scoped>
.bolus-layout { display: flex; flex-direction: column; }
.bolus-left { width: 100%; }

@media (min-width: 768px) {
  .bolus-layout {
    flex-direction: row;
    align-items: flex-start;
    gap: 4px;
  }
  .bolus-left { flex: 0 0 42%; }
  .doses-card { flex: 1; position: sticky; top: 8px; }
}

.dose-result { text-align: center; padding: 8px 0; }

.dose-total {
  font-size: 56px;
  font-weight: 700;
  color: var(--ion-color-primary);
  line-height: 1.1;
  transition: color 0.3s ease;
}
.dose-total.adjusted {
  color: var(--ion-color-warning);
}

.activity-factors {
  font-size: 12px;
  color: var(--ion-color-medium);
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.dose-breakdown {
  margin-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--ion-color-medium);
  font-size: 14px;
}

.breakdown-row {
  display: flex;
  justify-content: space-between;
  padding: 2px 16px;
}

.iob-row {
  border-top: 1px solid var(--ion-color-light);
  margin-top: 4px;
  padding-top: 6px;
}

.positive { color: var(--ion-color-danger); }
.negative { color: var(--ion-color-success); }

.timing-note { display: block; margin-top: 12px; font-size: 13px; font-weight: 500; }
.missing-note { display: block; margin-top: 8px; font-size: 13px; }
.activity-warning-note { display: block; margin-top: 8px; font-size: 13px; }
.dose-units { font-weight: 600; font-size: 16px; }
.dose-notes { font-style: italic; color: var(--ion-color-medium); }
ion-item { --background: transparent; }
.ai-estimate-row { --padding-start: 0; }
.breakdown-note { display: block; padding: 4px 16px 8px; font-size: 12px; line-height: 1.4; }
.recommendation-card { margin-top: 8px; }
.ai-icon { margin-right: 6px; vertical-align: middle; }
.safety-flag { display: inline-block; margin: 2px 4px 6px 0; padding: 2px 8px; border-radius: 12px; font-size: 12px; font-weight: 600; }
.rec-message { font-size: 14px; line-height: 1.6; margin-top: 8px; white-space: pre-wrap; }
.rec-placeholder { padding: 4px 0; }
</style>
