<template>
  <ion-app>
    <ion-split-pane content-id="main-content" when="md">
      <ion-menu content-id="main-content" type="overlay">
        <ion-header>
          <ion-toolbar color="primary">
            <ion-title>Smart Insulin</ion-title>
          </ion-toolbar>
        </ion-header>
        <ion-content>
          <ion-list lines="none">
            <ion-menu-toggle :auto-hide="false">
              <ion-item router-link="/dashboard" router-direction="root" lines="none" :detail="false" class="menu-item">
                <ion-icon slot="start" :icon="homeOutline" />
                <ion-label>{{ t('menu.dashboard') }}</ion-label>
              </ion-item>
            </ion-menu-toggle>
            <ion-menu-toggle :auto-hide="false">
              <ion-item router-link="/bolus" router-direction="root" lines="none" :detail="false" class="menu-item">
                <ion-icon slot="start" :icon="calculatorOutline" />
                <ion-label>{{ t('menu.bolus') }}</ion-label>
              </ion-item>
            </ion-menu-toggle>
            <ion-menu-toggle :auto-hide="false">
              <ion-item router-link="/meals" router-direction="root" lines="none" :detail="false" class="menu-item">
                <ion-icon slot="start" :icon="restaurantOutline" />
                <ion-label>{{ t('menu.meals') }}</ion-label>
              </ion-item>
            </ion-menu-toggle>
            <ion-menu-toggle :auto-hide="false">
              <ion-item router-link="/doses" router-direction="root" lines="none" :detail="false" class="menu-item">
                <ion-icon slot="start" :icon="medkitOutline" />
                <ion-label>{{ t('menu.doses') }}</ion-label>
              </ion-item>
            </ion-menu-toggle>
            <ion-menu-toggle :auto-hide="false">
              <ion-item router-link="/profile" router-direction="root" lines="none" :detail="false" class="menu-item">
                <ion-icon slot="start" :icon="personOutline" />
                <ion-label>{{ t('menu.profile') }}</ion-label>
              </ion-item>
            </ion-menu-toggle>
            <ion-menu-toggle :auto-hide="false">
              <ion-item lines="none" :detail="false" button @click="handleLogout" class="menu-item menu-item--logout">
                <ion-icon slot="start" :icon="logOutOutline" />
                <ion-label>{{ t('menu.logout') }}</ion-label>
              </ion-item>
            </ion-menu-toggle>
          </ion-list>
        </ion-content>
      </ion-menu>
      <ion-router-outlet id="main-content" />
    </ion-split-pane>
  </ion-app>
</template>

<style>
ion-menu ion-content {
  --padding-top: 8px;
}

ion-menu .menu-item {
  --border-radius: 8px;
  margin: 2px 8px;
  --padding-start: 12px;
}

ion-menu .menu-item--logout {
  --color: var(--ion-color-danger);
  margin-top: auto;
}
</style>

<script setup lang="ts">
import {
  IonApp,
  IonRouterOutlet,
  IonSplitPane,
  IonMenu,
  IonHeader,
  IonToolbar,
  IonTitle,
  IonContent,
  IonList,
  IonItem,
  IonIcon,
  IonLabel,
  IonMenuToggle,
} from '@ionic/vue';
import { homeOutline, personOutline, logOutOutline, calculatorOutline, restaurantOutline, medkitOutline } from 'ionicons/icons';
import { useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import { logout } from '@/services/auth';

const { t } = useI18n();

const router = useRouter();

async function handleLogout() {
  await logout();
  router.replace('/login');
}
</script>
