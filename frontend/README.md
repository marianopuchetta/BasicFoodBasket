# BasicFoodBasket Frontend

Frontend web application for **BasicFoodBasket** — a price tracking tool for the Argentine basic food basket.

Built with **Vite + React + TypeScript**, **Tailwind CSS v4**, **React Router v7**, and **react-i18next** (ES/EN).

---

## Prerequisites

- Node.js 18+
- npm 9+

---

## Local Development

```bash
cd frontend
npm install
npm run dev
```

The app will be available at [http://localhost:5173](http://localhost:5173).

---

## Production Build

```bash
npm run build
```

Output is placed in the `dist/` directory.

---

## Deploy to Netlify

### Option 1 — Netlify UI

1. Push the repository to GitHub.
2. In the [Netlify dashboard](https://app.netlify.com), click **Add new site → Import an existing project**.
3. Connect your GitHub repository.
4. Set the following build settings:
   - **Base directory:** `frontend`
   - **Build command:** `npm run build`
   - **Publish directory:** `frontend/dist`
5. Click **Deploy**.

SPA routing is handled by `public/_redirects` which instructs Netlify to serve `index.html` for all routes.

### Option 2 — netlify.toml (optional)

Add a `netlify.toml` at the repository root:

```toml
[build]
  base    = "frontend"
  command = "npm run build"
  publish = "dist"
```

---

## Project Structure

```
frontend/
├── public/
│   └── _redirects          # Netlify SPA routing
├── src/
│   ├── components/
│   │   └── Navbar.tsx      # Responsive navbar with i18n & language switcher
│   ├── i18n/
│   │   ├── index.ts        # i18next configuration
│   │   └── locales/
│   │       ├── en.json     # English translations
│   │       └── es.json     # Spanish translations
│   ├── pages/
│   │   ├── Home.tsx
│   │   ├── Login.tsx
│   │   └── SignUp.tsx
│   ├── App.tsx             # Router setup
│   ├── main.tsx            # Entry point
│   └── index.css           # Tailwind base styles
├── vite.config.ts
└── package.json
```

---

## i18n

The app supports **Spanish (ES)** and **English (EN)**. The active language can be toggled via the language button in the navbar. The default language is Spanish (`es`).

To add a new language:

1. Add a new JSON file to `src/i18n/locales/` (e.g., `pt.json`).
2. Import it in `src/i18n/index.ts` and add it to the `resources` object.
3. Add the locale code to `supportedLngs`.
