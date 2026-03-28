<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-menu-button />
        </ion-buttons>
        <ion-title>Profile</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding" :fullscreen="true">
      <div v-if="loading" class="ion-text-center ion-padding">
        <ion-spinner name="crescent" />
      </div>

      <form v-else @submit.prevent="saveProfile">
        <!-- Personal info -->
        <ion-card>
          <ion-card-header>
            <ion-card-title>Personal Information</ion-card-title>
          </ion-card-header>
          <ion-card-content>
            <ion-list lines="none">
              <ion-item>
                <ion-input
                  v-model="form.email"
                  label="Email"
                  label-placement="floating"
                  readonly
                  disabled
                />
              </ion-item>
              <ion-item>
                <ion-input
                  v-model="form.fullName"
                  label="Full Name"
                  label-placement="floating"
                  placeholder="John Doe"
                />
              </ion-item>
              <ion-item>
                <ion-select
                  v-model="form.diabetesType"
                  label="Diabetes Type"
                  label-placement="floating"
                  interface="action-sheet"
                  placeholder="Select"
                >
                  <ion-select-option :value="1">Type 1</ion-select-option>
                  <ion-select-option :value="2">Type 2</ion-select-option>
                </ion-select>
              </ion-item>
            </ion-list>
          </ion-card-content>
        </ion-card>

        <!-- Body measurements -->
        <ion-card>
          <ion-card-header>
            <ion-card-title>Body Measurements</ion-card-title>
          </ion-card-header>
          <ion-card-content>
            <ion-list lines="none">
              <ion-item>
                <ion-input
                  v-model.number="form.weightKg"
                  type="number"
                  label="Weight (kg)"
                  label-placement="floating"
                  step="0.1"
                />
              </ion-item>
              <ion-item>
                <ion-input
                  v-model.number="form.heightCm"
                  type="number"
                  label="Height (cm)"
                  label-placement="floating"
                  step="0.1"
                />
              </ion-item>
            </ion-list>
          </ion-card-content>
        </ion-card>

        <!-- Insulin settings -->
        <ion-card>
          <ion-card-header>
            <ion-card-title>Insulin Settings</ion-card-title>
          </ion-card-header>
          <ion-card-content>
            <ion-list lines="none">
              <ion-item>
                <ion-input
                  v-model.number="form.insulinSensitivityFactor"
                  type="number"
                  label="Insulin Sensitivity Factor (mmol/L per unit)"
                  label-placement="floating"
                  step="0.1"
                />
              </ion-item>
              <ion-item>
                <ion-input
                  v-model.number="form.insulinToCarbRatio"
                  type="number"
                  label="Insulin-to-Carb Ratio (g per unit)"
                  label-placement="floating"
                  step="0.1"
                />
              </ion-item>
              <ion-item>
                <ion-input
                  v-model.number="form.durationOfInsulinAction"
                  type="number"
                  label="Duration of Insulin Action (hours)"
                  label-placement="floating"
                  step="0.5"
                />
              </ion-item>
              <ion-item>
                <ion-input
                  v-model="form.basalInsulinType"
                  label="Basal Insulin Type"
                  label-placement="floating"
                  placeholder="e.g. Lantus, Tresiba"
                />
              </ion-item>
              <ion-item>
                <ion-input
                  v-model="form.bolusInsulinType"
                  label="Bolus Insulin Type"
                  label-placement="floating"
                  placeholder="e.g. NovoRapid, Humalog"
                />
              </ion-item>
            </ion-list>
          </ion-card-content>
        </ion-card>

        <!-- Target glucose range -->
        <ion-card>
          <ion-card-header>
            <ion-card-title>Target Glucose Range</ion-card-title>
          </ion-card-header>
          <ion-card-content>
            <ion-list lines="none">
              <ion-item>
                <ion-input
                  v-model.number="form.targetGlucoseMin"
                  type="number"
                  label="Min (mmol/L)"
                  label-placement="floating"
                  step="0.1"
                />
              </ion-item>
              <ion-item>
                <ion-input
                  v-model.number="form.targetGlucoseMax"
                  type="number"
                  label="Max (mmol/L)"
                  label-placement="floating"
                  step="0.1"
                />
              </ion-item>
            </ion-list>
          </ion-card-content>
        </ion-card>

        <ion-text v-if="errorMsg" color="danger" class="error-text">
          <p>{{ errorMsg }}</p>
        </ion-text>

        <ion-button expand="block" type="submit" :disabled="saving" class="ion-margin-top ion-margin-bottom">
          <ion-spinner v-if="saving" name="crescent" />
          <span v-else>Save Changes</span>
        </ion-button>
      </form>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import {
  IonPage,
  IonHeader,
  IonToolbar,
  IonButtons,
  IonMenuButton,
  IonTitle,
  IonContent,
  IonCard,
  IonCardHeader,
  IonCardTitle,
  IonCardContent,
  IonList,
  IonItem,
  IonInput,
  IonSelect,
  IonSelectOption,
  IonButton,
  IonText,
  IonSpinner,
  toastController,
} from '@ionic/vue';
import { apiFetch } from '@/services/api';

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
    const res = await apiFetch('/api/profile', {
      method: 'PUT',
      body: JSON.stringify(body),
    });

    if (res.ok) {
      const data = await res.json();
      form.value = { ...form.value, ...data };
      const toast = await toastController.create({ message: 'Profile saved', duration: 1500 });
      await toast.present();
    } else {
      const text = await res.text();
      let msg = 'Failed to save profile';
      try { msg = JSON.parse(text).message || msg; } catch { /* use default */ }
      errorMsg.value = msg;
    }
  } catch {
    errorMsg.value = 'Network error. Please try again.';
  } finally {
    saving.value = false;
  }
}

onMounted(loadProfile);
</script>

<style scoped>
ion-card {
  margin-bottom: 16px;
}

ion-item {
  --background: transparent;
  margin-bottom: 8px;
}

.error-text p {
  font-size: 14px;
  text-align: center;
  margin: 8px 16px;
}
</style>
