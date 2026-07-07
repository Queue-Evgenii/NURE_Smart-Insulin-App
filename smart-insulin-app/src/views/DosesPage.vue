<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-menu-button />
        </ion-buttons>
        <ion-title>{{ t('doses.title') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding" :fullscreen="true">
      <div class="page-wrapper">
        <div class="doses-layout">

          <ion-card>
            <ion-card-header>
              <ion-card-title>{{ t('doses.add.title') }}</ion-card-title>
            </ion-card-header>
            <ion-card-content>
              <ion-list lines="none">
                <ion-item>
                  <ion-input
                    v-model.number="newUnits"
                    type="number"
                    :label="t('doses.add.units')"
                    label-placement="floating"
                    placeholder="4.0"
                    step="0.5"
                    min="0"
                  />
                </ion-item>
                <ion-item>
                  <ion-select
                    v-model="newDoseType"
                    :label="t('doses.add.doseType')"
                    label-placement="floating"
                    interface="action-sheet"
                    :cancel-text="t('common.back')"
                  >
                    <ion-select-option value="BOLUS">{{ t('doses.add.bolus') }}</ion-select-option>
                    <ion-select-option value="BASAL">{{ t('doses.add.basal') }}</ion-select-option>
                    <ion-select-option value="CORRECTION">{{ t('doses.add.correction') }}</ion-select-option>
                  </ion-select>
                </ion-item>
                <ion-item>
                  <ion-input
                    v-model.number="newGlucoseBefore"
                    type="number"
                    :label="t('doses.add.glucoseBefore')"
                    label-placement="floating"
                    placeholder="7.2"
                    step="0.1"
                    min="0"
                  />
                </ion-item>
                <ion-item>
                  <ion-label>{{ t('common.dateTime') }}</ion-label>
                  <ion-datetime-button datetime="add-dose-time" slot="end" />
                  <ion-modal :keep-contents-mounted="true">
                    <ion-datetime id="add-dose-time" v-model="newInjectedAt" presentation="date-time" :max="nowMax" />
                  </ion-modal>
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
              <ion-button expand="block" :disabled="adding || !newUnits" @click="addDose" class="ion-margin-top">
                <ion-spinner v-if="adding" name="crescent" />
                <span v-else>{{ t('doses.add.button') }}</span>
              </ion-button>
            </ion-card-content>
          </ion-card>

          <ion-card class="list-card">
            <ion-card-header>
              <ion-card-title>{{ t('doses.list.title') }}</ion-card-title>
            </ion-card-header>
            <ion-card-content>
              <div v-if="loading" class="ion-text-center">
                <ion-spinner name="crescent" />
              </div>
              <ion-list v-else-if="doses.length > 0" lines="full">
                <ion-item-sliding v-for="d in doses" :key="d.id">
                  <ion-item>
                    <ion-label>
                      <h2>
                        <span class="dose-units">{{ d.doseUnits }} {{ t('common.unitInsulin') }}</span>
                        &nbsp;
                        <ion-badge :color="doseTypeColor(d.doseType)">{{ t(`doses.add.${d.doseType.toLowerCase()}`) }}</ion-badge>
                      </h2>
                      <p>{{ formatDate(d.injectedAt) }}{{ d.insulinType ? ` · ${d.insulinType}` : '' }}</p>
                      <p v-if="d.glucoseBefore != null">{{ t('doses.list.glucoseBefore', { value: d.glucoseBefore }) }}</p>
                      <p v-if="d.mealName">{{ t('doses.list.meal', { name: d.mealName }) }}</p>
                      <p v-if="d.notes" class="notes">{{ d.notes }}</p>
                    </ion-label>
                  </ion-item>
                  <ion-item-options side="end">
                    <ion-item-option color="medium" @click="openEdit(d)">
                      <ion-icon slot="icon-only" :icon="createOutline" />
                    </ion-item-option>
                    <ion-item-option color="danger" @click="deleteDose(d.id)">
                      <ion-icon slot="icon-only" :icon="trashOutline" />
                    </ion-item-option>
                  </ion-item-options>
                </ion-item-sliding>
              </ion-list>
              <div v-else class="ion-text-center">
                <p>{{ t('doses.list.empty') }}</p>
              </div>
              <ion-button v-if="hasMore" expand="block" fill="clear" :disabled="loading" @click="loadMore">
                {{ t('common.loadMore') }}
              </ion-button>
            </ion-card-content>
          </ion-card>

        </div>
      </div>

      <!-- Edit dose modal -->
      <ion-modal :is-open="editOpen" @did-dismiss="editOpen = false">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ t('doses.edit.title') }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="editOpen = false">{{ t('common.cancel') }}</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>
        <ion-content class="ion-padding">
          <ion-list lines="none">
            <ion-item>
              <ion-input v-model.number="editUnits" type="number" :label="t('doses.add.units')"
                label-placement="floating" step="0.5" min="0" />
            </ion-item>
            <ion-item>
              <ion-select v-model="editDoseType" :label="t('doses.add.doseType')" label-placement="floating"
                interface="action-sheet" :cancel-text="t('common.back')">
                <ion-select-option value="BOLUS">{{ t('doses.add.bolus') }}</ion-select-option>
                <ion-select-option value="BASAL">{{ t('doses.add.basal') }}</ion-select-option>
                <ion-select-option value="CORRECTION">{{ t('doses.add.correction') }}</ion-select-option>
              </ion-select>
            </ion-item>
            <ion-item>
              <ion-input v-model.number="editGlucoseBefore" type="number" :label="t('doses.add.glucoseBefore')"
                label-placement="floating" step="0.1" min="0" />
            </ion-item>
            <ion-item>
              <ion-label>{{ t('common.dateTime') }}</ion-label>
              <ion-datetime-button datetime="edit-dose-time" slot="end" />
              <ion-modal :keep-contents-mounted="true">
                <ion-datetime id="edit-dose-time" v-model="editInjectedAt" presentation="date-time" :max="nowMax" />
              </ion-modal>
            </ion-item>
            <ion-item>
              <ion-input v-model="editNotes" type="text" :label="t('common.notes')" label-placement="floating" />
            </ion-item>
          </ion-list>
          <ion-button expand="block" :disabled="saving" @click="saveEdit" class="ion-margin-top">
            <ion-spinner v-if="saving" name="crescent" />
            <span v-else>{{ t('common.save') }}</span>
          </ion-button>
        </ion-content>
      </ion-modal>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  IonPage, IonHeader, IonToolbar, IonButtons, IonMenuButton, IonTitle, IonContent,
  IonCard, IonCardHeader, IonCardTitle, IonCardContent,
  IonList, IonItem, IonItemSliding, IonItemOptions, IonItemOption,
  IonLabel, IonInput, IonSelect, IonSelectOption, IonBadge,
  IonButton, IonSpinner, IonIcon, IonModal, IonDatetime, IonDatetimeButton,
  toastController,
} from '@ionic/vue';
import { trashOutline, createOutline } from 'ionicons/icons';
import { apiFetch } from '@/services/api';
import { RANGES, firstError, presentValidationError } from '@/utils/validation';
import { nowLocalISO, apiToLocalISO, localToApiISO } from '@/utils/datetime';
import { confirmAction } from '@/utils/confirm';

const { t } = useI18n();

interface InsulinDose {
  id: number;
  doseUnits: number;
  doseType: string;
  insulinType: string | null;
  injectedAt: string;
  mealRecordId: number | null;
  mealName: string | null;
  glucoseBefore: number | null;
  notes: string | null;
  createdAt: string;
}

const doses = ref<InsulinDose[]>([]);
const loading = ref(false);
const currentPage = ref(0);
const hasMore = ref(true);

const newUnits = ref<number | null>(null);
const newDoseType = ref('BOLUS');
const newInsulinType = ref('');
const newGlucoseBefore = ref<number | null>(null);
const newNotes = ref('');
const newInjectedAt = ref(nowLocalISO());
const adding = ref(false);

// Upper bound for datetime pickers: no future entries allowed
const nowMax = nowLocalISO();

// Edit modal state
const editOpen = ref(false);
const editId = ref<number | null>(null);
const editUnits = ref<number | null>(null);
const editDoseType = ref('BOLUS');
const editGlucoseBefore = ref<number | null>(null);
const editNotes = ref('');
const editInjectedAt = ref(nowLocalISO());
// preserved silently (not shown in the edit form) so it is not wiped on update
const editInsulinType = ref<string | null>(null);
const editMealRecordId = ref<number | null>(null);
const saving = ref(false);

function formatDate(iso: string): string { return new Date(iso).toLocaleString(); }

function doseTypeColor(type: string): string {
  if (type === 'BOLUS') return 'primary';
  if (type === 'BASAL') return 'secondary';
  return 'warning';
}

async function loadDoses(page = 0) {
  loading.value = true;
  try {
    const res = await apiFetch(`/api/doses?page=${page}&size=20`);
    if (res.ok) {
      const data = await res.json();
      if (page === 0) doses.value = data.content;
      else doses.value.push(...data.content);
      hasMore.value = !data.last;
      currentPage.value = page;
    }
  } finally {
    loading.value = false;
  }
}

function loadMore() { loadDoses(currentPage.value + 1); }

async function addDose() {
  const err = firstError([
    [newUnits.value, RANGES.doseUnits],
    [newGlucoseBefore.value, RANGES.glucose, { required: false }],
  ]);
  if (err) { await presentValidationError(t(err.key, err.params ?? {})); return; }
  adding.value = true;
  try {
    const injectedAt = localToApiISO(newInjectedAt.value);

    if (newGlucoseBefore.value) {
      await apiFetch('/api/glucose', {
        method: 'POST',
        body: JSON.stringify({
          glucoseValue: newGlucoseBefore.value,
          measurementType: 'MANUAL',
          measuredAt: injectedAt,
        }),
      });
    }

    const res = await apiFetch('/api/doses', {
      method: 'POST',
      body: JSON.stringify({
        doseUnits: newUnits.value,
        doseType: newDoseType.value,
        insulinType: newInsulinType.value || null,
        injectedAt,
        glucoseBefore: newGlucoseBefore.value || null,
        notes: newNotes.value || null,
      }),
    });
    if (res.ok) {
      newUnits.value = null;
      newInsulinType.value = '';
      newGlucoseBefore.value = null;
      newNotes.value = '';
      newInjectedAt.value = nowLocalISO();
      await loadDoses(0);
      const toast = await toastController.create({ message: t('doses.add.success'), duration: 1500 });
      await toast.present();
    } else {
      const toast = await toastController.create({ message: t('doses.add.error'), duration: 2000, color: 'danger' });
      await toast.present();
    }
  } finally {
    adding.value = false;
  }
}

function openEdit(d: InsulinDose) {
  editId.value = d.id;
  editUnits.value = d.doseUnits;
  editDoseType.value = d.doseType;
  editGlucoseBefore.value = d.glucoseBefore;
  editNotes.value = d.notes ?? '';
  editInjectedAt.value = apiToLocalISO(d.injectedAt);
  editInsulinType.value = d.insulinType;
  editMealRecordId.value = d.mealRecordId;
  editOpen.value = true;
}

async function saveEdit() {
  if (editId.value == null) return;
  const err = firstError([
    [editUnits.value, RANGES.doseUnits],
    [editGlucoseBefore.value, RANGES.glucose, { required: false }],
  ]);
  if (err) { await presentValidationError(t(err.key, err.params ?? {})); return; }
  saving.value = true;
  try {
    const res = await apiFetch(`/api/doses/${editId.value}`, {
      method: 'PUT',
      body: JSON.stringify({
        doseUnits: editUnits.value,
        doseType: editDoseType.value,
        insulinType: editInsulinType.value,
        injectedAt: localToApiISO(editInjectedAt.value),
        mealRecordId: editMealRecordId.value,
        glucoseBefore: editGlucoseBefore.value || null,
        notes: editNotes.value || null,
      }),
    });
    if (res.ok) {
      editOpen.value = false;
      await loadDoses(0);
      const toast = await toastController.create({ message: t('common.saved'), duration: 1500 });
      await toast.present();
    } else {
      const toast = await toastController.create({ message: t('doses.add.error'), duration: 2000, color: 'danger' });
      await toast.present();
    }
  } finally {
    saving.value = false;
  }
}

async function deleteDose(id: number) {
  const confirmed = await confirmAction({
    header: t('common.confirmDeleteTitle'),
    message: t('common.confirmDeleteMessage'),
    confirmText: t('common.delete'),
    cancelText: t('common.cancel'),
  });
  if (!confirmed) return;
  const res = await apiFetch(`/api/doses/${id}`, { method: 'DELETE' });
  if (res.ok) doses.value = doses.value.filter(d => d.id !== id);
}

onMounted(() => loadDoses(0));
</script>

<style scoped>
.doses-layout { display: flex; flex-direction: column; }

@media (min-width: 768px) {
  .doses-layout {
    flex-direction: row;
    align-items: flex-start;
    gap: 4px;
  }
  .doses-layout > ion-card:first-child { flex: 0 0 42%; }
  .list-card { flex: 1; position: sticky; top: 8px; }
}

ion-item { --background: transparent; }
.dose-units { font-weight: 600; font-size: 16px; }
.notes { font-style: italic; color: var(--ion-color-medium); }
</style>
