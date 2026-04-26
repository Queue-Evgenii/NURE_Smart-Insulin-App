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
} from '@ionic/vue';
import { trashOutline } from 'ionicons/icons';
import { apiFetch } from '@/services/api';

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

async function deleteMeal(id: number) {
  const res = await apiFetch(`/api/meals/${id}`, { method: 'DELETE' });
  if (res.ok) meals.value = meals.value.filter(m => m.id !== id);
}

onMounted(() => loadMeals(0));
</script>

<style scoped>
ion-item { --background: transparent; }
.notes { font-style: italic; color: var(--ion-color-medium); }
</style>
