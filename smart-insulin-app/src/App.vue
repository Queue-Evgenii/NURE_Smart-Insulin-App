<template>
  <ion-app>
    <ion-split-pane content-id="main-content">
      <ion-menu content-id="main-content" type="overlay">
        <ion-header>
          <ion-toolbar color="primary">
            <ion-title>Smart Insulin</ion-title>
          </ion-toolbar>
        </ion-header>
        <ion-content>
          <ion-list>
            <ion-menu-toggle auto-hide="true">
              <ion-item router-link="/dashboard" router-direction="root" lines="none" detail="false">
                <ion-icon slot="start" :icon="homeOutline" />
                <ion-label>Dashboard</ion-label>
              </ion-item>
            </ion-menu-toggle>
            <ion-menu-toggle auto-hide="true">
              <ion-item router-link="/profile" router-direction="root" lines="none" detail="false">
                <ion-icon slot="start" :icon="personOutline" />
                <ion-label>Profile</ion-label>
              </ion-item>
            </ion-menu-toggle>
            <ion-menu-toggle auto-hide="true">
              <ion-item lines="none" detail="false" button @click="handleLogout">
                <ion-icon slot="start" :icon="logOutOutline" />
                <ion-label>Log Out</ion-label>
              </ion-item>
            </ion-menu-toggle>
          </ion-list>
        </ion-content>
      </ion-menu>
      <ion-router-outlet id="main-content" />
    </ion-split-pane>
  </ion-app>
</template>

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
import { homeOutline, personOutline, logOutOutline } from 'ionicons/icons';
import { useRouter } from 'vue-router';
import { logout } from '@/services/auth';

const router = useRouter();

async function handleLogout() {
  await logout();
  router.replace('/login');
}
</script>
