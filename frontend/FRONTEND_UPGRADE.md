# Frontend Upgrade Notes

## Motion / chart libs
- Element Plus transitions + dark css-vars
- ECharts via `ChartBox.vue`
- `@formkit/auto-animate` (v-auto-animate)
- `@vueuse/core` (available for theme/pref helpers)
- CSS route transition: `fade-slide` (240ms)

## New / enhanced pages
- Login (animated gradient)
- Dashboard (stat cards + trend/pie + timeline)
- Approvals (timeline side panel)
- Inquiry compare (quote cards + bar chart + AI highlight)
- AI Assistant (`/ai`)
- Supplier Profile (`/suppliers/:id/profile`)
- Process Overview (`/process`)
- Notifications (`/notifications`)
- Profile / theme (`/profile`)
- Budget, Contracts, Returns, Invoices, Supplier Audits, Templates, Anomaly Insight

## Mock switch
Personal center: "失败回退 mock" (default on). Extended modules call `/api/*` then fall back to `src/mock/data.js`.

## Preview
```bash
cd frontend && npm install && npm run dev -- --host 0.0.0.0 --port 5173
```
Proxy `/api` -> `http://127.0.0.1:8080`.
