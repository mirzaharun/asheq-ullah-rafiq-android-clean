/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        royal: {
          50: '#EFF6FF',
          100: '#DBEAFE',
          600: '#1D4ED8',
          700: '#1E40AF',
          800: '#0D47A1',
          900: '#0A2E6E',
        },
        forest: {
          600: '#059669',
          700: '#006A4E',
          800: '#004D38',
        },
      },
      fontFamily: {
        sans: ['"Hind Siliguri"', 'Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
