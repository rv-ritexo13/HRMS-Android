# HRMS Admin Dashboard

A static web console for administering the HRMS Supabase backend — employees,
leave approvals, expense approvals, salary/payslips, and content (announcements,
holidays, notifications). No build step: plain HTML/CSS/JS + `supabase-js` from a
CDN.

## Run it

It talks to Supabase over HTTPS, so serve it over `http://` (don't just
double-click `index.html` — some browsers block `file://` origins):

```bash
cd admin-web
python3 -m http.server 8000
# open http://localhost:8000
```

Or deploy the folder as-is to any static host (Netlify drop, Vercel, GitHub
Pages, Supabase Storage). Nothing server-side is required.

## Sign in

| role | email | password |
|------|-------|----------|
| Admin | `admin@hrms.app` | `admin123456` |

Only accounts flagged in `public.admins` can use the dashboard — it calls the
`is_admin()` RPC after login and signs you out if you're not an admin.

## How access is enforced

- The browser only ever holds the **anon key** (in `config.js`) — safe to ship.
- Every read/write goes through PostgREST with **Row Level Security**:
  - per-employee tables (`profiles`, `expenses`, `leave_requests`,
    `leave_balances`, `payslips`, `performance_reviews`, `goals`, `kras`,
    `notifications`) are visible to their owner **or** an admin;
  - global tables (`holidays`, `announcements`) are readable by any signed-in
    user but writable only by admins;
  - an anonymous caller (no login) sees nothing.
- So even though the anon key is public, a non-admin session cannot read or edit
  anyone else's data.

## Creating employees

The Employees tab has an **+ Add** button. It creates a real login account +
profile via the `create-employee` Supabase Edge Function
(`supabase/functions/create-employee/`), which runs the service_role key
server-side — the browser never sees it, and the function refuses anyone who
isn't an admin. Give the new hire the temporary password you set; they log into
the mobile app with it.

## Known limits (first version)

- There is no `attendance` table yet, so attendance isn't an admin tab.
- Config lives in `config.js`. To point at another project, edit the URL + anon
  key there.
