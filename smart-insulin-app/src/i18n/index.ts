import { createI18n } from 'vue-i18n';
import en from './locales/en';
import uk from './locales/uk';

const LOCALE_KEY = 'app_locale';

export type AppLocale = 'en' | 'uk';

export function getSavedLocale(): AppLocale {
  return (localStorage.getItem(LOCALE_KEY) as AppLocale) ?? 'uk';
}

export function saveLocale(locale: AppLocale) {
  localStorage.setItem(LOCALE_KEY, locale);
}

const i18n = createI18n({
  legacy: false,
  locale: getSavedLocale(),
  fallbackLocale: 'en',
  messages: { en, uk },
});

export default i18n;
