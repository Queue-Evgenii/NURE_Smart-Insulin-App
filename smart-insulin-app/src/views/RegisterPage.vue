<template>
  <ion-page>
    <ion-content class="ion-padding" :fullscreen="true">
      <div class="auth-container">
        <div class="auth-header">
          <ion-icon :icon="medkit" class="auth-logo" />
          <h1>Create Account</h1>
          <p>Join Smart Insulin today</p>
        </div>

        <form @submit.prevent="handleRegister">
          <ion-list lines="none">
            <ion-item>
              <ion-input
                v-model="fullName"
                type="text"
                label="Full Name"
                label-placement="floating"
                placeholder="John Doe"
                autocomplete="name"
                required
              />
            </ion-item>

            <ion-item>
              <ion-input
                v-model="email"
                type="email"
                label="Email"
                label-placement="floating"
                placeholder="your@email.com"
                autocomplete="email"
                required
              />
            </ion-item>

            <ion-item>
              <ion-input
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                label="Password"
                label-placement="floating"
                placeholder="••••••••"
                autocomplete="new-password"
                required
              />
              <ion-button
                slot="end"
                fill="clear"
                size="small"
                @click="showPassword = !showPassword"
                type="button"
              >
                <ion-icon :icon="showPassword ? eyeOff : eye" slot="icon-only" />
              </ion-button>
            </ion-item>

            <ion-item>
              <ion-input
                v-model="confirmPassword"
                :type="showPassword ? 'text' : 'password'"
                label="Confirm Password"
                label-placement="floating"
                placeholder="••••••••"
                autocomplete="new-password"
                required
              />
            </ion-item>

            <ion-item>
              <ion-select
                v-model="diabetesType"
                label="Diabetes Type"
                label-placement="floating"
                placeholder="Select type (optional)"
                interface="action-sheet"
              >
                <ion-select-option :value="1">Type 1</ion-select-option>
                <ion-select-option :value="2">Type 2</ion-select-option>
              </ion-select>
            </ion-item>
          </ion-list>

          <ion-text v-if="errorMsg" color="danger" class="error-text">
            <p>{{ errorMsg }}</p>
          </ion-text>

          <ion-button
            expand="block"
            type="submit"
            :disabled="loading"
            class="ion-margin-top"
          >
            <ion-spinner v-if="loading" name="crescent" />
            <span v-else>Create Account</span>
          </ion-button>
        </form>

        <div class="auth-footer">
          <p>
            Already have an account?
            <router-link to="/login">Sign in</router-link>
          </p>
        </div>
      </div>
    </ion-content>
  </ion-page>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  IonPage,
  IonContent,
  IonList,
  IonItem,
  IonInput,
  IonButton,
  IonText,
  IonIcon,
  IonSpinner,
  IonSelect,
  IonSelectOption,
} from '@ionic/vue';
import { medkit, eye, eyeOff } from 'ionicons/icons';
import { register } from '@/services/auth';

const router = useRouter();

const fullName = ref('');
const email = ref('');
const password = ref('');
const confirmPassword = ref('');
const diabetesType = ref<number | undefined>(undefined);
const showPassword = ref(false);
const loading = ref(false);
const errorMsg = ref('');

async function handleRegister() {
  if (!fullName.value || !email.value || !password.value || !confirmPassword.value) {
    errorMsg.value = 'Please fill in all required fields.';
    return;
  }

  if (password.value.length < 6) {
    errorMsg.value = 'Password must be at least 6 characters.';
    return;
  }

  if (password.value !== confirmPassword.value) {
    errorMsg.value = 'Passwords do not match.';
    return;
  }

  loading.value = true;
  errorMsg.value = '';

  const result = await register(
    email.value,
    password.value,
    fullName.value,
    diabetesType.value,
  );

  loading.value = false;

  if (result.ok) {
    router.replace('/tabs/tab1');
  } else {
    errorMsg.value = result.error;
  }
}
</script>

<style scoped>
.auth-container {
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-height: 100%;
  max-width: 420px;
  margin: 0 auto;
  padding: 16px;
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.auth-logo {
  font-size: 64px;
  color: var(--ion-color-primary);
  margin-bottom: 8px;
}

.auth-header h1 {
  margin: 8px 0 4px;
  font-size: 28px;
  font-weight: 700;
}

.auth-header p {
  color: var(--ion-color-medium);
  margin: 0;
}

ion-list {
  background: transparent;
}

ion-item {
  --background: var(--ion-color-light);
  --border-radius: 12px;
  margin-bottom: 12px;
}

.error-text p {
  font-size: 14px;
  text-align: center;
  margin: 8px 0;
}

.auth-footer {
  text-align: center;
  margin-top: 24px;
}

.auth-footer a {
  color: var(--ion-color-primary);
  text-decoration: none;
  font-weight: 600;
}
</style>
