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
                <div class="dose-total">{{ result.totalDose.toFixed(1) }} {{ t('common.unitInsulin') }}</div>
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
  IonList, IonItem, IonLabel, IonBadge, IonInput, IonButton, IonSpinner, IonNote,
  toastController,
} from '@ionic/vue';
import { apiFetch } from '@/services/api';

const { t } = useI18n();

interface BolusResult {
  bolusForCarbs: number;
  correctionDose: number;
  currentIob: number;
  totalDose: number;
  mealRecordId: number | null;
  missingParams: string[];
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

async function calculate() {
  if (!currentGlucose.value || carbsG.value === null) return;
  calculating.value = true;
  result.value = null;
  try {
    const res = await apiFetch('/api/bolus/calculate', {
      method: 'POST',
      body: JSON.stringify({ currentGlucose: currentGlucose.value, carbsG: carbsG.value }),
    });
    if (res.ok) {
      result.value = await res.json();
      adjustedDose.value = result.value!.totalDose;
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
      adjustedDose.value = null;
      currentGlucose.value = null;
      mealName.value = '';
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
.dose-units { font-weight: 600; font-size: 16px; }
.dose-notes { font-style: italic; color: var(--ion-color-medium); }
ion-item { --background: transparent; }
</style>
