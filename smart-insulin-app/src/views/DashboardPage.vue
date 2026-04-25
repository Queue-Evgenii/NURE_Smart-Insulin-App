<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-menu-button />
        </ion-buttons>
        <ion-title>Dashboard</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding" :fullscreen="true">
      <div class="page-wrapper">
        <div class="dashboard-layout">

          <!-- Left column -->
          <div class="dashboard-left">
            <!-- HbA1c Card -->
            <ion-card>
              <ion-card-header>
                <ion-card-title>HbA1c Estimate</ion-card-title>
                <ion-card-subtitle>Last 90 days</ion-card-subtitle>
              </ion-card-header>
              <ion-card-content>
                <div v-if="hba1cLoading" class="ion-text-center">
                  <ion-spinner name="crescent" />
                </div>
                <div v-else-if="hba1c" class="hba1c-display">
                  <div class="hba1c-value" :class="hba1cColor">{{ hba1c.hba1c.toFixed(1) }}%</div>
                  <div class="hba1c-details">
                    <span>Avg glucose: {{ hba1c.averageGlucose.toFixed(1) }} mmol/L</span>
                    <span>{{ hba1c.readingsCount }} readings</span>
                  </div>
                </div>
                <div v-else class="ion-text-center">
                  <p>No readings in the last 90 days</p>
                </div>
              </ion-card-content>
            </ion-card>

            <!-- Add reading -->
            <ion-card>
              <ion-card-header>
                <ion-card-title>Add Glucose Reading</ion-card-title>
              </ion-card-header>
              <ion-card-content>
                <ion-list lines="none">
                  <ion-item>
                    <ion-input
                      v-model.number="newGlucose"
                      type="number"
                      label="Glucose (mmol/L)"
                      label-placement="floating"
                      placeholder="5.5"
                      step="0.1"
                      min="0"
                    />
                  </ion-item>
                  <ion-item>
                    <ion-select
                      v-model="newMeasurementType"
                      label="Measurement Type"
                      label-placement="floating"
                      interface="action-sheet"
                    >
                      <ion-select-option value="MANUAL">Manual</ion-select-option>
                      <ion-select-option value="CGM">CGM</ion-select-option>
                    </ion-select>
                  </ion-item>
                  <ion-item>
                    <ion-input
                      v-model="newNotes"
                      type="text"
                      label="Notes (optional)"
                      label-placement="floating"
                      placeholder="Before lunch..."
                    />
                  </ion-item>
                </ion-list>
                <ion-button expand="block" :disabled="addingReading || !newGlucose" @click="addReading" class="ion-margin-top">
                  <ion-spinner v-if="addingReading" name="crescent" />
                  <span v-else>Add Reading</span>
                </ion-button>
              </ion-card-content>
            </ion-card>
          </div>

          <!-- Right column -->
          <div class="dashboard-right">
            <ion-card class="readings-card">
              <ion-card-header>
                <ion-card-title>Recent Readings</ion-card-title>
              </ion-card-header>
              <ion-card-content>
                <div v-if="readingsLoading" class="ion-text-center">
                  <ion-spinner name="crescent" />
                </div>
                <ion-list v-else-if="readings.length > 0" lines="full">
                  <ion-item-sliding v-for="r in readings" :key="r.id">
                    <ion-item>
                      <ion-label>
                        <h2 :class="glucoseColor(r.glucoseValue)">{{ r.glucoseValue.toFixed(1) }} mmol/L</h2>
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
                  <p>No readings yet. Add your first one above!</p>
                </div>

                <ion-button
                  v-if="hasMore"
                  expand="block"
                  fill="clear"
                  @click="loadMore"
                  :disabled="readingsLoading"
                >
                  Load More
                </ion-button>
              </ion-card-content>
            </ion-card>
          </div>

        </div>
      </div>
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
  IonCardSubtitle,
  IonCardContent,
  IonList,
  IonItem,
  IonItemSliding,
  IonItemOptions,
  IonItemOption,
  IonLabel,
  IonInput,
  IonSelect,
  IonSelectOption,
  IonButton,
  IonIcon,
  IonSpinner,
  toastController,
} from '@ionic/vue';
import { trashOutline } from 'ionicons/icons';
import { apiFetch } from '@/services/api';

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

// ── State ──
const readings = ref<GlucoseReading[]>([]);
const readingsLoading = ref(false);
const currentPage = ref(0);
const hasMore = ref(true);

const hba1c = ref<HbA1cData | null>(null);
const hba1cLoading = ref(false);

const newGlucose = ref<number | null>(null);
const newMeasurementType = ref('MANUAL');
const newNotes = ref('');
const addingReading = ref(false);

// ── Computed color ──
const hba1cColor = ref('');

function glucoseColor(value: number): string {
  if (value < 4.0) return 'glucose-low';
  if (value > 10.0) return 'glucose-high';
  return 'glucose-normal';
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleString();
}

// ── Load data ──
async function loadReadings(page = 0) {
  readingsLoading.value = true;
  try {
    const res = await apiFetch(`/api/glucose?page=${page}&size=20`);
    if (res.ok) {
      const data = await res.json();
      if (page === 0) {
        readings.value = data.content;
      } else {
        readings.value.push(...data.content);
      }
      hasMore.value = !data.last;
      currentPage.value = page;
    }
  } finally {
    readingsLoading.value = false;
  }
}

function loadMore() {
  loadReadings(currentPage.value + 1);
}

async function loadHbA1c() {
  hba1cLoading.value = true;
  try {
    const to = new Date().toISOString();
    const from = new Date(Date.now() - 90 * 24 * 60 * 60 * 1000).toISOString();
    const res = await apiFetch('/api/glucose/hba1c', {
      method: 'POST',
      body: JSON.stringify({ from, to }),
    });
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

// ── Actions ──
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
      await Promise.all([loadReadings(0), loadHbA1c()]);
      const toast = await toastController.create({ message: 'Reading added', duration: 1500 });
      await toast.present();
    } else {
      const toast = await toastController.create({ message: 'Failed to add reading', duration: 2000, color: 'danger' });
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

onMounted(() => {
  loadReadings(0);
  loadHbA1c();
});
</script>

<style scoped>
.hba1c-display {
  text-align: center;
}

.hba1c-value {
  font-size: 48px;
  font-weight: 700;
  line-height: 1.2;
}

.hba1c-details {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-top: 8px;
  color: var(--ion-color-medium);
  font-size: 14px;
}

.hba1c-good { color: var(--ion-color-success); }
.hba1c-moderate { color: var(--ion-color-warning); }
.hba1c-high { color: var(--ion-color-danger); }

.glucose-low { color: var(--ion-color-warning); }
.glucose-normal { color: var(--ion-color-success); }
.glucose-high { color: var(--ion-color-danger); }

.notes {
  font-style: italic;
  color: var(--ion-color-medium);
}

ion-card {
  margin-bottom: 16px;
}

ion-item {
  --background: transparent;
}

/* Desktop layout */
.dashboard-layout {
  display: flex;
  flex-direction: column;
}

.dashboard-left,
.dashboard-right {
  width: 100%;
}

.readings-card {
  height: auto;
}

@media (min-width: 768px) {
  .dashboard-layout {
    flex-direction: row;
    align-items: flex-start;
    gap: 4px;
  }

  .dashboard-left {
    flex: 0 0 42%;
  }

  .dashboard-right {
    flex: 1;
  }

  .readings-card {
    position: sticky;
    top: 8px;
  }
}
</style>
