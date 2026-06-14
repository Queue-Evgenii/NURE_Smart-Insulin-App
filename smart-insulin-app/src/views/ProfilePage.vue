<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-menu-button />
        </ion-buttons>
        <ion-title>{{ t('profile.title') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding" :fullscreen="true">
      <div v-if="loading" class="ion-text-center ion-padding">
        <ion-spinner name="crescent" />
      </div>

      <form v-else @submit.prevent="saveProfile" class="page-wrapper">
        <div class="profile-grid">

          <!-- Left column -->
          <div class="profile-col">

            <!-- App settings -->
            <ion-card>
              <ion-card-header>
                <ion-card-title>{{ t('profile.settings.title') }}</ion-card-title>
              </ion-card-header>
              <ion-card-content>
                <ion-list lines="none">
                  <ion-item>
                    <ion-select
                      v-model="currentLocale"
                      :label="t('profile.settings.language')"
                      label-placement="floating"
                      interface="action-sheet"
                      :cancel-text="t('common.back')"
                      @ion-change="changeLocale"
                    >
                      <ion-select-option value="uk">Українська</ion-select-option>
                      <ion-select-option value="en">English</ion-select-option>
                    </ion-select>
                  </ion-item>
                  <ion-item>
                    <ion-select
                      v-model="currentTheme"
                      :label="t('profile.settings.theme')"
                      label-placement="floating"
                      interface="action-sheet"
                      :cancel-text="t('common.back')"
                      @ion-change="changeTheme"
                    >
                      <ion-select-option value="system">{{ t('profile.settings.themeSystem') }}</ion-select-option>
                      <ion-select-option value="light">{{ t('profile.settings.themeLight') }}</ion-select-option>
                      <ion-select-option value="dark">{{ t('profile.settings.themeDark') }}</ion-select-option>
                    </ion-select>
                  </ion-item>
                </ion-list>
              </ion-card-content>
            </ion-card>

            <ion-card>
              <ion-card-header>
                <ion-card-title>{{ t('profile.personal.title') }}</ion-card-title>
              </ion-card-header>
              <ion-card-content>
                <ion-list lines="none">
                  <ion-item>
                    <ion-input v-model="form.email" label="Email" label-placement="floating" readonly disabled />
                  </ion-item>
                  <ion-item>
                    <ion-input
                      v-model="form.fullName"
                      :label="t('profile.personal.fullName')"
                      label-placement="floating"
                      :placeholder="t('profile.personal.fullNamePlaceholder')"
                    />
                  </ion-item>
                  <!-- diabetes type has no effect on calculations — hidden -->
                  <!--
                  <ion-item>
                    <ion-select
                      v-model="form.diabetesType"
                      :label="t('profile.personal.diabetesType')"
                      label-placement="floating"
                      interface="action-sheet"
                      :cancel-text="t('common.back')"
                      :placeholder="t('profile.personal.select')"
                    >
                      <ion-select-option :value="1">{{ t('profile.personal.type1') }}</ion-select-option>
                      <ion-select-option :value="2">{{ t('profile.personal.type2') }}</ion-select-option>
                    </ion-select>
                  </ion-item>
                  -->
                </ion-list>
              </ion-card-content>
            </ion-card>

            <ion-card>
              <ion-card-header>
                <ion-card-title>{{ t('profile.body.title') }}</ion-card-title>
              </ion-card-header>
              <ion-card-content>
                <ion-list lines="none">
                  <ion-item>
                    <ion-input v-model.number="form.weightKg" type="number" :label="t('profile.body.weight')" label-placement="floating" step="0.1" />
                  </ion-item>
                  <ion-item>
                    <ion-input v-model.number="form.heightCm" type="number" :label="t('profile.body.height')" label-placement="floating" step="0.1" />
                  </ion-item>
                </ion-list>
              </ion-card-content>
            </ion-card>
          </div>

          <!-- Right column -->
          <div class="profile-col">
            <ion-card>
              <ion-card-header>
                <ion-card-title>{{ t('profile.insulin.title') }}</ion-card-title>
              </ion-card-header>
              <ion-card-content>
                <ion-list lines="none">
                  <ion-item>
                    <ion-input v-model.number="form.insulinSensitivityFactor" type="number"
                      :label="t('profile.insulin.isf')" label-placement="floating"
                      :helper-text="t('profile.insulin.isfHint')"
                      step="0.1" min="0.5" max="20" />
                  </ion-item>
                  <ion-item>
                    <ion-input v-model.number="form.insulinToCarbRatio" type="number"
                      :label="t('profile.insulin.icr')" label-placement="floating"
                      :helper-text="t('profile.insulin.icrHint')"
                      step="0.5" min="1" max="50" />
                  </ion-item>
                  <ion-item>
                    <ion-input v-model.number="form.durationOfInsulinAction" type="number"
                      :label="t('profile.insulin.dia')" label-placement="floating"
                      :helper-text="t('profile.insulin.diaHint')"
                      step="0.5" min="1" max="12" />
                  </ion-item>
                  <ion-item>
                    <ion-input v-model.number="form.insulinResistanceFactor" type="number"
                      :label="t('profile.insulin.irf')" label-placement="floating"
                      :helper-text="t('profile.insulin.irfHint')"
                      step="0.1" min="0.5" max="5.0" />
                  </ion-item>
                  <ion-item>
                    <ion-input v-model.number="form.carbAbsorptionMinutes" type="number"
                      :label="t('profile.insulin.cam')" label-placement="floating"
                      :helper-text="t('profile.insulin.camHint')"
                      step="10" min="60" max="240" />
                  </ion-item>
                  <ion-item>
                    <ion-select
                      v-model="form.basalInsulinType"
                      :label="t('profile.insulin.basalType')"
                      label-placement="floating"
                      interface="action-sheet"
                      :cancel-text="t('common.back')"
                      :placeholder="t('profile.insulin.selectPlaceholder')"
                    >
                      <ion-select-option v-for="ins in BASAL_INSULINS" :key="ins.name" :value="ins.name">
                        {{ ins.name }}
                      </ion-select-option>
                    </ion-select>
                  </ion-item>
                  <ion-item>
                    <ion-select
                      v-model="form.bolusInsulinType"
                      :label="t('profile.insulin.bolusType')"
                      label-placement="floating"
                      interface="action-sheet"
                      :cancel-text="t('common.back')"
                      :placeholder="t('profile.insulin.selectPlaceholder')"
                      @ion-change="onBolusInsulinChange"
                    >
                      <ion-select-option v-for="ins in BOLUS_INSULINS" :key="ins.name" :value="ins.name">
                        {{ ins.name }} — DIA {{ ins.diahours }}{{ t('profile.insulin.hours') }}
                      </ion-select-option>
                    </ion-select>
                  </ion-item>
                  <ion-note v-if="diaAutoFilled" color="primary" class="dia-note">
                    {{ t('profile.insulin.diaAutoFilled', { insulin: form.bolusInsulinType }) }}
                  </ion-note>
                </ion-list>
              </ion-card-content>
            </ion-card>

            <ion-card>
              <ion-card-header>
                <ion-card-title>{{ t('profile.targetGlucose.title') }}</ion-card-title>
              </ion-card-header>
              <ion-card-content>
                <ion-list lines="none">
                  <ion-item>
                    <ion-input v-model.number="form.targetGlucoseMin" type="number" :label="t('profile.targetGlucose.min')" label-placement="floating" step="0.1" />
                  </ion-item>
                  <ion-item>
                    <ion-input v-model.number="form.targetGlucoseMax" type="number" :label="t('profile.targetGlucose.max')" label-placement="floating" step="0.1" />
                  </ion-item>
                </ion-list>
              </ion-card-content>
            </ion-card>
          </div>

        </div>

        <!-- Activity coefficients table — full width below the two-column grid -->
        <ion-card>
          <ion-card-header>
            <ion-card-title>{{ t('profile.activity.title') }}</ion-card-title>
          </ion-card-header>
          <ion-card-content>
            <p class="activity-hint">{{ t('profile.activity.hint') }}</p>
            <div class="coeff-table">
              <!-- header row -->
              <div class="coeff-cell coeff-header"></div>
              <div v-for="atype in ACTIVITY_TYPES" :key="atype" class="coeff-cell coeff-header">
                {{ t(`profile.activity.${atype}`) }}
              </div>
              <!-- data rows -->
              <template v-for="intensity in ACTIVITY_INTENSITIES" :key="intensity">
                <div class="coeff-cell coeff-label">{{ t(`profile.activity.${intensity}`) }}</div>
                <div v-for="atype in ACTIVITY_TYPES" :key="atype" class="coeff-cell">
                  <ion-input
                    :value="form.activityCoefficients[coeffKey(intensity, atype)]"
                    @ion-input="onCoeffInput(intensity, atype, $event)"
                    type="number"
                    step="0.01"
                    min="0.10"
                    max="1.00"
                    class="coeff-input"
                  />
                </div>
              </template>
            </div>
            <ion-button fill="outline" size="small" class="ion-margin-top" @click="resetCoefficients">
              {{ t('profile.activity.reset') }}
            </ion-button>
          </ion-card-content>
        </ion-card>

        <ion-text v-if="errorMsg" color="danger" class="error-text">
          <p>{{ errorMsg }}</p>
        </ion-text>

        <ion-button expand="block" type="submit" :disabled="saving" class="ion-margin-top ion-margin-bottom save-btn">
          <ion-spinner v-if="saving" name="crescent" />
          <span v-else>{{ t('profile.save') }}</span>
        </ion-button>
      </form>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  IonPage, IonHeader, IonToolbar, IonButtons, IonMenuButton, IonTitle, IonContent,
  IonCard, IonCardHeader, IonCardTitle, IonCardContent,
  IonList, IonItem, IonInput, IonSelect, IonSelectOption,
  IonButton, IonText, IonSpinner, IonNote,
  toastController,
} from '@ionic/vue';
import { apiFetch } from '@/services/api';
import { type AppLocale, saveLocale, getSavedLocale } from '@/i18n';
import { type ThemeMode, applyTheme, getSavedTheme } from '@/services/theme';

const { t, locale } = useI18n();

const currentLocale = ref<AppLocale>(getSavedLocale());
const currentTheme = ref<ThemeMode>(getSavedTheme());

// ── Insulin presets ──────────────────────────────────────────────────────────

interface InsulinPreset { name: string; diahours: number; }

const BOLUS_INSULINS: InsulinPreset[] = [
  { name: 'NovoRapid (Aspart)',   diahours: 4.0 },
  { name: 'Humalog (Lispro)',     diahours: 3.5 },
  { name: 'Apidra (Glulisine)',   diahours: 4.0 },
  { name: 'Fiasp (Ultra-fast)',   diahours: 4.0 },
  { name: 'Lyumjev',              diahours: 5.0 },
  { name: 'Actrapid (Regular)',   diahours: 6.0 },
];

const BASAL_INSULINS: InsulinPreset[] = [
  { name: 'Lantus (Glargine)',      diahours: 24 },
  { name: 'Tresiba (Degludec)',     diahours: 42 },
  { name: 'Levemir (Detemir)',      diahours: 18 },
  { name: 'Toujeo (Glargine U300)', diahours: 36 },
  { name: 'Basaglar (Glargine)',    diahours: 24 },
];

const diaAutoFilled = ref(false);

function onBolusInsulinChange(event: CustomEvent) {
  const selected = event.detail.value as string;
  const preset = BOLUS_INSULINS.find(i => i.name === selected);
  if (preset) {
    form.value.durationOfInsulinAction = preset.diahours;
    diaAutoFilled.value = true;
  }
}

// ── Activity coefficients table ──────────────────────────────────────────────

const ACTIVITY_INTENSITIES = ['light', 'moderate', 'high', 'maximal'] as const;
const ACTIVITY_TYPES = ['aerobic', 'anaerobic', 'mixed'] as const;
type Intensity = typeof ACTIVITY_INTENSITIES[number];
type AType = typeof ACTIVITY_TYPES[number];

type ActivityCoefficients = {
  lightAerobic: number; lightAnaerobic: number; lightMixed: number;
  moderateAerobic: number; moderateAnaerobic: number; moderateMixed: number;
  highAerobic: number; highAnaerobic: number; highMixed: number;
  maximalAerobic: number; maximalAnaerobic: number; maximalMixed: number;
};

const DEFAULT_COEFFICIENTS: ActivityCoefficients = {
  lightAerobic: 0.90, lightAnaerobic: 0.95, lightMixed: 0.93,
  moderateAerobic: 0.75, moderateAnaerobic: 0.87, moderateMixed: 0.82,
  highAerobic: 0.60, highAnaerobic: 0.77, highMixed: 0.68,
  maximalAerobic: 0.40, maximalAnaerobic: 0.67, maximalMixed: 0.52,
};

function coeffKey(intensity: Intensity, atype: AType): keyof ActivityCoefficients {
  return (intensity + atype.charAt(0).toUpperCase() + atype.slice(1)) as keyof ActivityCoefficients;
}

function onCoeffInput(intensity: Intensity, atype: AType, event: CustomEvent) {
  const val = parseFloat((event.target as HTMLInputElement).value);
  if (!isNaN(val)) {
    form.value.activityCoefficients[coeffKey(intensity, atype)] = val;
  }
}

function resetCoefficients() {
  form.value.activityCoefficients = { ...DEFAULT_COEFFICIENTS };
}

// ── Form ─────────────────────────────────────────────────────────────────────

interface ProfileForm {
  email: string;
  fullName: string;
  diabetesType: number | null;
  weightKg: number | null;
  heightCm: number | null;
  insulinSensitivityFactor: number | null;
  insulinToCarbRatio: number | null;
  targetGlucoseMin: number | null;
  targetGlucoseMax: number | null;
  durationOfInsulinAction: number | null;
  insulinResistanceFactor: number;
  carbAbsorptionMinutes: number;
  basalInsulinType: string | null;
  bolusInsulinType: string | null;
  activityCoefficients: ActivityCoefficients;
}

const form = ref<ProfileForm>({
  email: '',
  fullName: '',
  diabetesType: null,
  weightKg: null,
  heightCm: null,
  insulinSensitivityFactor: null,
  insulinToCarbRatio: null,
  targetGlucoseMin: null,
  targetGlucoseMax: null,
  durationOfInsulinAction: null,
  insulinResistanceFactor: 1.0,
  carbAbsorptionMinutes: 120,
  basalInsulinType: null,
  bolusInsulinType: null,
  activityCoefficients: { ...DEFAULT_COEFFICIENTS },
});

const loading = ref(true);
const saving = ref(false);
const errorMsg = ref('');

// ── Locale / theme ────────────────────────────────────────────────────────────

function changeLocale(event: CustomEvent) {
  const selected = event.detail.value as AppLocale;
  locale.value = selected;
  saveLocale(selected);
}

function changeTheme(event: CustomEvent) {
  const selected = event.detail.value as ThemeMode;
  currentTheme.value = selected;
  applyTheme(selected);
}

// ── Load / save ───────────────────────────────────────────────────────────────

async function loadProfile() {
  loading.value = true;
  try {
    const res = await apiFetch('/api/profile');
    if (res.ok) {
      const data = await res.json();
      form.value = {
        ...form.value,
        ...data,
        activityCoefficients: data.activityCoefficients ?? { ...DEFAULT_COEFFICIENTS },
      };
    }
  } finally {
    loading.value = false;
  }
}

function validateForm(): string | null {
  const f = form.value;
  if (f.insulinSensitivityFactor != null && (f.insulinSensitivityFactor < 0.5 || f.insulinSensitivityFactor > 20))
    return t('profile.validation.isfRange');
  if (f.insulinToCarbRatio != null && (f.insulinToCarbRatio < 1 || f.insulinToCarbRatio > 50))
    return t('profile.validation.icrRange');
  if (f.durationOfInsulinAction != null && (f.durationOfInsulinAction < 1 || f.durationOfInsulinAction > 12))
    return t('profile.validation.diaRange');
  if (f.targetGlucoseMin != null && f.targetGlucoseMax != null && f.targetGlucoseMin >= f.targetGlucoseMax)
    return t('profile.validation.targetOrder');
  if (f.insulinResistanceFactor < 0.5 || f.insulinResistanceFactor > 5.0)
    return t('profile.validation.irfRange');
  if (f.carbAbsorptionMinutes < 60 || f.carbAbsorptionMinutes > 240)
    return t('profile.validation.camRange');
  // validate coefficients range
  for (const intensity of ACTIVITY_INTENSITIES) {
    for (const atype of ACTIVITY_TYPES) {
      const v = f.activityCoefficients[coeffKey(intensity, atype)];
      if (v < 0.10 || v > 1.00) return t('profile.validation.coeffRange');
    }
  }
  return null;
}

async function saveProfile() {
  saving.value = true;
  errorMsg.value = '';
  const validationError = validateForm();
  if (validationError) {
    errorMsg.value = validationError;
    saving.value = false;
    return;
  }
  try {
    const body = { ...form.value };
    delete (body as Record<string, unknown>).email;
    const res = await apiFetch('/api/profile', { method: 'PUT', body: JSON.stringify(body) });
    if (res.ok) {
      const data = await res.json();
      form.value = {
        ...form.value,
        ...data,
        activityCoefficients: data.activityCoefficients ?? { ...DEFAULT_COEFFICIENTS },
      };
      const toast = await toastController.create({ message: t('profile.saved'), duration: 1500 });
      await toast.present();
    } else {
      const text = await res.text();
      let msg = t('profile.errorSave');
      try { msg = JSON.parse(text).message || msg; } catch { /* use default */ }
      errorMsg.value = msg;
    }
  } catch {
    errorMsg.value = t('profile.errorNetwork');
  } finally {
    saving.value = false;
  }
}

onMounted(loadProfile);
</script>

<style scoped>
ion-card { margin-bottom: 16px; }
ion-item { --background: transparent; margin-bottom: 8px; }
.dia-note { display: block; padding: 4px 16px 8px; font-size: 12px; }
.error-text p { font-size: 14px; text-align: center; margin: 8px 16px; }
.activity-hint { font-size: 12px; color: var(--ion-color-medium); margin: 0 0 12px; }

.profile-grid { display: flex; flex-direction: column; }
.profile-col { width: 100%; }

/* Activity coefficients 4×3 grid */
.coeff-table {
  display: grid;
  grid-template-columns: 100px repeat(3, 1fr);
  gap: 4px;
  overflow-x: auto;
}
.coeff-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2px;
}
.coeff-header {
  font-size: 12px;
  font-weight: 600;
  text-align: center;
  color: var(--ion-color-medium);
  padding: 4px 2px;
}
.coeff-label {
  font-size: 12px;
  font-weight: 500;
  justify-content: flex-start;
  padding-left: 4px;
}
.coeff-input {
  --padding-start: 4px;
  --padding-end: 4px;
  font-size: 13px;
  text-align: center;
  min-width: 0;
}

@media (min-width: 768px) {
  .profile-grid {
    flex-direction: row;
    align-items: flex-start;
    gap: 4px;
  }
  .profile-col { flex: 1; }
  .save-btn { max-width: 320px; margin-left: auto; margin-right: auto; display: block; }
}
</style>
