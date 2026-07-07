<template>
  <ion-page>
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start">
          <ion-menu-button />
        </ion-buttons>
        <ion-title>{{ t('meals.title') }}</ion-title>
      </ion-toolbar>
    </ion-header>

    <ion-content class="ion-padding" :fullscreen="true">
      <div class="page-wrapper">

        <ion-card>
          <ion-card-header>
            <ion-card-title>{{ t('meals.list.title') }}</ion-card-title>
          </ion-card-header>
          <ion-card-content>
            <div v-if="loading" class="ion-text-center">
              <ion-spinner name="crescent" />
            </div>
            <ion-list v-else-if="meals.length > 0" lines="full">
              <ion-item-sliding v-for="m in meals" :key="m.id">
                <ion-item>
                  <ion-label>
                    <h2>{{ m.mealName || t('meals.list.defaultName') }}</h2>
                    <p>{{ m.carbohydratesG }} {{ t('meals.list.carbsUnit') }}{{ m.glycemicIndex ? ` · ${t('meals.list.gi')} ${m.glycemicIndex}` : '' }}</p>
                    <p>{{ formatDate(m.mealTime) }}</p>
                    <p v-if="m.notes" class="notes">{{ m.notes }}</p>
                  </ion-label>
                </ion-item>
                <ion-item-options side="end">
                  <ion-item-option color="medium" @click="openEdit(m)">
                    <ion-icon slot="icon-only" :icon="createOutline" />
                  </ion-item-option>
                  <ion-item-option color="danger" @click="deleteMeal(m.id)">
                    <ion-icon slot="icon-only" :icon="trashOutline" />
                  </ion-item-option>
                </ion-item-options>
              </ion-item-sliding>
            </ion-list>
            <div v-else class="ion-text-center ion-padding">
              <p>{{ t('meals.list.empty') }}</p>
            </div>
            <ion-button v-if="hasMore" expand="block" fill="clear" :disabled="loading" @click="loadMore">
              {{ t('common.loadMore') }}
            </ion-button>
          </ion-card-content>
        </ion-card>

      </div>

      <!-- Edit meal modal -->
      <ion-modal :is-open="editOpen" @did-dismiss="editOpen = false">
        <ion-header>
          <ion-toolbar>
            <ion-title>{{ t('meals.edit.title') }}</ion-title>
            <ion-buttons slot="end">
              <ion-button @click="editOpen = false">{{ t('common.cancel') }}</ion-button>
            </ion-buttons>
          </ion-toolbar>
        </ion-header>
        <ion-content class="ion-padding">
          <ion-list lines="none">
            <ion-item>
              <ion-input v-model="editName" type="text" :label="t('bolus.form.mealName')" label-placement="floating" />
            </ion-item>
            <ion-item>
              <ion-input v-model.number="editCarbs" type="number" :label="t('bolus.form.carbs')"
                label-placement="floating" step="1" min="0" />
            </ion-item>
            <ion-item>
              <ion-input v-model.number="editGi" type="number" :label="t('bolus.form.gi')"
                label-placement="floating" step="1" min="1" max="100" />
            </ion-item>
            <ion-item>
              <ion-label>{{ t('common.dateTime') }}</ion-label>
              <ion-datetime-button datetime="edit-meal-time" slot="end" />
              <ion-modal :keep-contents-mounted="true">
                <ion-datetime id="edit-meal-time" v-model="editMealTime" presentation="date-time" :max="nowMax" />
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
  IonLabel, IonButton, IonSpinner, IonIcon,
  IonModal, IonInput, IonDatetime, IonDatetimeButton,
  toastController,
} from '@ionic/vue';
import { trashOutline, createOutline } from 'ionicons/icons';
import { apiFetch } from '@/services/api';
import { RANGES, firstError, presentValidationError } from '@/utils/validation';
import { nowLocalISO, apiToLocalISO, localToApiISO } from '@/utils/datetime';
import { confirmAction } from '@/utils/confirm';

const { t } = useI18n();

interface MealRecord {
  id: number;
  mealName: string | null;
  carbohydratesG: number;
  glycemicIndex: number | null;
  mealTime: string;
  notes: string | null;
}

const meals = ref<MealRecord[]>([]);
const loading = ref(false);
const currentPage = ref(0);
const hasMore = ref(true);

const nowMax = nowLocalISO();

// Edit modal state
const editOpen = ref(false);
const editId = ref<number | null>(null);
const editName = ref('');
const editCarbs = ref<number | null>(null);
const editGi = ref<number | null>(null);
const editMealTime = ref(nowLocalISO());
const editNotes = ref('');
const saving = ref(false);

function formatDate(iso: string): string { return new Date(iso).toLocaleString(); }

async function loadMeals(page = 0) {
  loading.value = true;
  try {
    const res = await apiFetch(`/api/meals?page=${page}&size=20`);
    if (res.ok) {
      const data = await res.json();
      if (page === 0) meals.value = data.content;
      else meals.value.push(...data.content);
      hasMore.value = !data.last;
      currentPage.value = page;
    }
  } finally {
    loading.value = false;
  }
}

function loadMore() { loadMeals(currentPage.value + 1); }

function openEdit(m: MealRecord) {
  editId.value = m.id;
  editName.value = m.mealName ?? '';
  editCarbs.value = m.carbohydratesG;
  editGi.value = m.glycemicIndex;
  editMealTime.value = apiToLocalISO(m.mealTime);
  editNotes.value = m.notes ?? '';
  editOpen.value = true;
}

async function saveEdit() {
  if (editId.value == null) return;
  const err = firstError([
    [editCarbs.value, RANGES.carbs],
    [editGi.value, RANGES.glycemicIndex, { required: false }],
  ]);
  if (err) { await presentValidationError(t(err.key, err.params ?? {})); return; }
  saving.value = true;
  try {
    const res = await apiFetch(`/api/meals/${editId.value}`, {
      method: 'PUT',
      body: JSON.stringify({
        mealName: editName.value || null,
        carbohydratesG: editCarbs.value,
        glycemicIndex: editGi.value || null,
        mealTime: localToApiISO(editMealTime.value),
        notes: editNotes.value || null,
      }),
    });
    if (res.ok) {
      editOpen.value = false;
      await loadMeals(0);
      const toast = await toastController.create({ message: t('common.saved'), duration: 1500 });
      await toast.present();
    } else {
      const toast = await toastController.create({ message: t('meals.edit.error'), duration: 2000, color: 'danger' });
      await toast.present();
    }
  } finally {
    saving.value = false;
  }
}

async function deleteMeal(id: number) {
  const confirmed = await confirmAction({
    header: t('common.confirmDeleteTitle'),
    message: t('common.confirmDeleteMessage'),
    confirmText: t('common.delete'),
    cancelText: t('common.cancel'),
  });
  if (!confirmed) return;
  const res = await apiFetch(`/api/meals/${id}`, { method: 'DELETE' });
  if (res.ok) meals.value = meals.value.filter(m => m.id !== id);
}

onMounted(() => loadMeals(0));
</script>

<style scoped>
ion-item { --background: transparent; }
.notes { font-style: italic; color: var(--ion-color-medium); }
</style>
