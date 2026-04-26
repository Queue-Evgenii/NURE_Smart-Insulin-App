import { createRouter, createWebHistory } from '@ionic/vue-router';
import { RouteRecordRaw } from 'vue-router';
import { isAuthenticated, initAuth } from '@/services/auth';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    component: () => import('@/views/LoginPage.vue'),
    meta: { public: true },
  },
  {
    path: '/register',
    component: () => import('@/views/RegisterPage.vue'),
    meta: { public: true },
  },
  {
    path: '/dashboard',
    component: () => import('@/views/DashboardPage.vue'),
  },
  {
    path: '/bolus',
    component: () => import('@/views/BolusPage.vue'),
  },
  {
    path: '/meals',
    component: () => import('@/views/MealsPage.vue'),
  },
  {
    path: '/doses',
    component: () => import('@/views/DosesPage.vue'),
  },
  {
    path: '/profile',
    component: () => import('@/views/ProfilePage.vue'),
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

let authInitialized = false;

router.beforeEach(async (to) => {
  if (!authInitialized) {
    await initAuth();
    authInitialized = true;
  }

  const isPublic = to.meta.public === true;

  if (!isPublic && !isAuthenticated.value) {
    return '/login';
  }

  if (isPublic && isAuthenticated.value && (to.path === '/login' || to.path === '/register')) {
    return '/dashboard';
  }
});

export default router;
