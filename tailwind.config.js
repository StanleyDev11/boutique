/**
 * Config Tailwind pour build LOCAL/offline.
 * Remplace le compilateur JIT navigateur (cdn.tailwindcss.com) par un CSS statique
 * compile en scannant les templates Thymeleaf.
 * Les couleurs custom reprennent les tailwind.config inline des templates
 * (login.html, ouverture-caisse.html, ho.html).
 */
module.exports = {
  content: [
    './src/main/resources/templates/**/*.html',
  ],
  // Classes potentiellement injectees dynamiquement par JS (securite).
  safelist: [
    'hidden',
    'fa-bars', 'fa-times', 'fa-eye', 'fa-eye-slash',
    'fa-chevron-right', 'fa-chevron-down',
    'bg-gray-100',
    'border-blue-500', 'text-blue-600', 'border-blue-600',
    'border-red-500', 'text-red-600',
    'bg-green-100', 'text-green-800', 'bg-red-100', 'text-red-800',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#3B82F6',
          dark: '#2563eb',
          50: '#f0f9ff',
          100: '#e0f2fe',
          200: '#bae6fd',
          300: '#7dd3fc',
          400: '#38bdf8',
          500: '#0ea5e9',
          600: '#0284c7',
          700: '#0369a1',
          800: '#075985',
          900: '#0c4a6e',
        },
        secondary: '#10B981',
        supermarket: {
          primary: '#2563eb',
          accent: '#059669',
          light: '#f0f9ff',
        },
      },
      animation: {
        gradient: 'gradient 8s linear infinite',
        float: 'float 3s ease-in-out infinite',
      },
      keyframes: {
        gradient: {
          '0%, 100%': { 'background-size': '200% 200%', 'background-position': 'left center' },
          '50%': { 'background-size': '200% 200%', 'background-position': 'right center' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0)' },
          '50%': { transform: 'translateY(-10px)' },
        },
      },
    },
  },
  plugins: [],
};
