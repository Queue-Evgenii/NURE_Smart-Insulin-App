export type ThemeMode = 'light' | 'dark' | 'system';

const STORAGE_KEY = 'app-theme';
const DARK_CLASS = 'ion-palette-dark';

export function getSavedTheme(): ThemeMode {
  return (localStorage.getItem(STORAGE_KEY) as ThemeMode) ?? 'system';
}

export function applyTheme(mode?: ThemeMode) {
  const resolved = mode ?? getSavedTheme();
  if (mode) localStorage.setItem(STORAGE_KEY, mode);

  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  const useDark = resolved === 'dark' || (resolved === 'system' && prefersDark);

  document.documentElement.classList.toggle(DARK_CLASS, useDark);
}
