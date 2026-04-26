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
                  <ion-item>
                    <ion-select
                      v-model="form.diabetesType"
                      :label="t('profile.personal.diabetesType')"
                      label-placement="floating"
                      interface="action-sheet"
                      :placeholder="t('profile.personal.select')"
                    >
                      <ion-select-option :value="1">{{ t('profile.personal.type1') }}</ion-select-option>
                      <ion-select-option :value="2">{{ t('profile.personal.type2') }}</ion-select-option>
                    </ion-select>
                  </ion-item>
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
                      @ion-change="changeLocale"
                    >
                      <ion-select-option value="uk">Українська</ion-select-option>
                      <ion-select-option value="en">English</ion-select-option>
                    </ion-select>
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
                    <ion-input v-model.number="form.insulinSensitivityFactor" type="number" :label="t('profile.insulin.isf')" label-placement="floating" step="0.1" />
                  </ion-item>
                  <ion-item>
                    <ion-input v-model.number="form.insulinToCarbRatio" type="number" :label="t('profile.insulin.icr')" label-placement="floating" step="0.1" />
                  </ion-item>
                  <ion-item>
                    <ion-input v-model.number="form.durationOfInsulinAction" type="number" :label="t('profile.insulin.dia')" label-placement="floating" step="0.5" />
                  </ion-item>
                  <ion-item>
                    <ion-input v-model="form.basalInsulinType" :label="t('profile.insulin.basalType')" label-placement="floating" :placeholder="t('profile.insulin.basalPlaceholder')" />
                  </ion-item>
                  <ion-item>
                    <ion-input v-model="form.bolusInsulinType" :label="t('profile.insulin.bolusType')" label-placement="floating" :placeholder="t('profile.insulin.bolusPlaceholder')" />
                  </ion-item>
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
  IonButton, IonText, IonSpinner,
  toastController,
} from '@ionic/vue';
import { apiFetch } from '@/services/api';
import { type AppLocale, saveLocale, getSavedLocale } from '@/i18n';

const { t, locale } = useI18n();

const currentLocale = ref<AppLocale>(getSavedLocale());

function changeLocale(event: CustomEvent) {
  const selected = event.detail.value as AppLocale;
  locale.value = selected;
  saveLocale(selected);
}

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
  basalInsulinType: string | null;
  bolusInsulinType: string | null;
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
  basalInsulinType: null,
  bolusInsulinType: null,
});

const loading = ref(true);
const saving = ref(false);
const errorMsg = ref('');

async function loadProfile() {
  loading.value = true;
  try {
    const res = await apiFetch('/api/profile');
    if (res.ok) {
      const data = await res.json();
      form.value = { ...form.value, ...data };
    }
  } finally {
    loading.value = false;
  }
}

async function saveProfile() {
  saving.value = true;
  errorMsg.value = '';
  try {
    const body = { ...form.value };
    delete (body as Record<string, unknown>).email;
    const res = await apiFetch('/api/profile', { method: 'PUT', body: JSON.stringify(body) });
    if (res.ok) {
      const data = await res.json();
      form.value = { ...form.value, ...data };
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

.error-text p { font-size: 14px; text-align: center; margin: 8px 16px; }

.profile-grid { display: flex; flex-direction: column; }
.profile-col { width: 100%; }

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
