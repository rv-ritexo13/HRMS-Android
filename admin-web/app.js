/* HRMS Admin dashboard — static SPA on top of Supabase (supabase-js v2).
 * Auth: Supabase email/password. Access gate: the is_admin() RPC. All data
 * access goes through PostgREST with RLS, so a non-admin session can't read or
 * write anyone else's rows even though the anon key is public. */

const cfg = window.HRMS_CONFIG;
const sb = window.supabase.createClient(cfg.SUPABASE_URL, cfg.SUPABASE_ANON_KEY);

// ---------- formatting helpers ----------
const rupees = (n) =>
  "₹" + Number(n || 0).toLocaleString("en-IN");
const asDate = (ms) =>
  ms ? new Date(Number(ms)).toLocaleDateString("en-IN", { day: "numeric", month: "short", year: "numeric" }) : "—";
const esc = (s) =>
  String(s ?? "").replace(/[&<>"]/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]));

function statusKind(s) {
  switch (String(s || "").toUpperCase()) {
    case "APPROVED": case "PAID": return "success";
    case "SUBMITTED": case "PENDING": case "IN_PROGRESS": return "warning";
    case "REJECTED": case "CANCELLED": return "danger";
    case "DRAFT": return "muted";
    default: return "info";
  }
}
const gross = (r) => Number(r.basic) + Number(r.hra) + Number(r.special_allowance) + Number(r.other_allowances);
const net = (r) => gross(r) - (Number(r.provident_fund) + Number(r.professional_tax) + Number(r.other_deductions));

// ---------- entity definitions ----------
const ENTITIES = {
  employees: {
    title: "Employees", table: "profiles", pk: "employee_id", order: { col: "full_name", asc: true },
    columns: [
      { k: "employee_id", label: "ID" }, { k: "full_name", label: "Name" },
      { k: "department", label: "Dept" }, { k: "designation", label: "Designation" },
      { k: "work_email", label: "Email" }, { k: "phone", label: "Phone" },
    ],
    search: ["employee_id", "full_name", "department", "designation", "work_email"],
    canAdd: false, canDelete: false, canEdit: true,
    fields: [
      { k: "full_name", label: "Full name" }, { k: "department", label: "Department" },
      { k: "designation", label: "Designation" }, { k: "reporting_manager", label: "Manager" },
      { k: "work_email", label: "Work email" }, { k: "phone", label: "Phone" },
      { k: "office_location", label: "Office location" },
    ],
  },
  leave: {
    title: "Leave Requests", table: "leave_requests", pk: "id", order: { col: "applied_millis", asc: false },
    columns: [
      { k: "leave_type", label: "Type" }, { k: "start_millis", label: "From", fmt: "date" },
      { k: "end_millis", label: "To", fmt: "date" }, { k: "days", label: "Days" },
      { k: "status", label: "Status", fmt: "status" }, { k: "reason", label: "Reason" },
    ],
    search: ["leave_type", "reason", "status"],
    canAdd: false, canDelete: false, canEdit: false,
    statusFlow: { Approve: "APPROVED", Reject: "REJECTED" }, commentField: "manager_comments",
  },
  expenses: {
    title: "Expenses", table: "expenses", pk: "id", order: { col: "expense_date_millis", asc: false },
    columns: [
      { k: "title", label: "Title" }, { k: "category", label: "Category" },
      { k: "amount", label: "Amount", fmt: "rupees" }, { k: "expense_date_millis", label: "Date", fmt: "date" },
      { k: "status", label: "Status", fmt: "status" },
    ],
    search: ["title", "category", "status"],
    canAdd: false, canDelete: false, canEdit: false,
    statusFlow: { Approve: "APPROVED", Reject: "REJECTED", "Mark Paid": "PAID" },
  },
  salary: {
    title: "Salary / Payslips", table: "payslips", pk: "month_key", order: { col: "month_key", asc: false },
    columns: [
      { k: "month_key", label: "Month" }, { k: "employee_name", label: "Employee" },
      { k: "basic", label: "Basic", fmt: "rupees" }, { k: "hra", label: "HRA", fmt: "rupees" },
      { k: "__gross", label: "Gross", fmt: "rupees", calc: gross }, { k: "__net", label: "Net", fmt: "rupees", calc: net },
    ],
    search: ["month_key", "employee_name"],
    canAdd: false, canDelete: false, canEdit: true,
    fields: [
      { k: "basic", label: "Basic", type: "number" }, { k: "hra", label: "HRA", type: "number" },
      { k: "special_allowance", label: "Special allowance", type: "number" },
      { k: "other_allowances", label: "Other allowances", type: "number" },
      { k: "provident_fund", label: "Provident fund", type: "number" },
      { k: "professional_tax", label: "Professional tax", type: "number" },
      { k: "other_deductions", label: "Other deductions", type: "number" },
    ],
  },
  announcements: {
    title: "Announcements", table: "announcements", pk: "id", order: { col: "sort_order", asc: true },
    columns: [{ k: "title", label: "Title" }, { k: "body", label: "Body" }, { k: "time_label", label: "When" }],
    search: ["title", "body"], canAdd: true, canDelete: true, canEdit: true, idPrefix: "ANN-",
    fields: [
      { k: "title", label: "Title" }, { k: "body", label: "Body", type: "textarea" },
      { k: "time_label", label: "Time label" }, { k: "sort_order", label: "Order", type: "number" },
    ],
  },
  holidays: {
    title: "Holidays", table: "holidays", pk: "id", order: { col: "sort_order", asc: true },
    columns: [
      { k: "day_number", label: "Day" }, { k: "month_abbrev", label: "Month" },
      { k: "name", label: "Name" }, { k: "day_of_week", label: "Weekday" },
    ],
    search: ["name", "month_abbrev"], canAdd: true, canDelete: true, canEdit: true, idPrefix: "HOL-",
    fields: [
      { k: "day_number", label: "Day (e.g. 02)" }, { k: "month_abbrev", label: "Month (e.g. OCT)" },
      { k: "name", label: "Name" }, { k: "day_of_week", label: "Weekday" },
      { k: "sort_order", label: "Order", type: "number" },
    ],
  },
  notifications: {
    title: "Notifications", table: "notifications", pk: "id", order: { col: "sort_order", asc: true },
    columns: [
      { k: "title", label: "Title" }, { k: "body", label: "Body" },
      { k: "time_label", label: "When" }, { k: "is_read", label: "Read", fmt: "bool" },
    ],
    search: ["title", "body"], canAdd: true, canDelete: true, canEdit: true, idPrefix: "NOTIF-",
    fields: [
      { k: "title", label: "Title" }, { k: "body", label: "Body", type: "textarea" },
      { k: "time_label", label: "Time label" }, { k: "is_read", label: "Read", type: "checkbox" },
      { k: "sort_order", label: "Order", type: "number" },
    ],
  },
};
const ORDER = ["employees", "leave", "expenses", "salary", "announcements", "holidays", "notifications"];

// ---------- state + elements ----------
let currentKey = "employees";
let currentRows = [];
const $ = (id) => document.getElementById(id);

// ---------- auth ----------
async function boot() {
  const { data } = await sb.auth.getSession();
  if (data.session && (await isAdmin())) showApp();
  else showLogin();
}

async function isAdmin() {
  const { data, error } = await sb.rpc("is_admin");
  return !error && data === true;
}

$("loginForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  $("loginError").textContent = "";
  $("loginBtn").disabled = true;
  const { error } = await sb.auth.signInWithPassword({ email: $("email").value, password: $("password").value });
  if (error) { $("loginError").textContent = error.message; $("loginBtn").disabled = false; return; }
  if (!(await isAdmin())) {
    await sb.auth.signOut();
    $("loginError").textContent = "This account is not an admin.";
    $("loginBtn").disabled = false;
    return;
  }
  $("loginBtn").disabled = false;
  showApp();
});

$("logoutBtn").addEventListener("click", async () => { await sb.auth.signOut(); showLogin(); });

function showLogin() { $("appView").classList.add("hidden"); $("loginView").classList.remove("hidden"); }
async function showApp() {
  $("loginView").classList.add("hidden");
  $("appView").classList.remove("hidden");
  const { data } = await sb.auth.getUser();
  $("whoami").textContent = data?.user?.email || "";
  renderNav();
  select(currentKey);
}

// ---------- navigation ----------
function renderNav() {
  const nav = $("nav");
  nav.innerHTML = "";
  ORDER.forEach((key) => {
    const b = document.createElement("button");
    b.textContent = ENTITIES[key].title;
    b.className = key === currentKey ? "active" : "";
    b.onclick = () => select(key);
    nav.appendChild(b);
  });
}

async function select(key) {
  currentKey = key;
  renderNav();
  const ent = ENTITIES[key];
  $("pageTitle").textContent = ent.title;
  $("search").value = "";
  $("addBtn").classList.toggle("hidden", !ent.canAdd);
  await load();
}

$("refreshBtn").addEventListener("click", load);
$("search").addEventListener("input", () => renderTable());
$("addBtn").addEventListener("click", () => openModal(null));

// ---------- data ----------
async function load() {
  const ent = ENTITIES[currentKey];
  $("tableWrap").innerHTML = '<div class="empty">Loading…</div>';
  const { data, error } = await sb.from(ent.table).select("*").order(ent.order.col, { ascending: ent.order.asc });
  if (error) { $("tableWrap").innerHTML = `<div class="empty">Error: ${esc(error.message)}</div>`; return; }
  currentRows = data || [];
  renderTable();
}

function renderTable() {
  const ent = ENTITIES[currentKey];
  const q = $("search").value.trim().toLowerCase();
  let rows = currentRows;
  if (q && ent.search) rows = rows.filter((r) => ent.search.some((k) => String(r[k] ?? "").toLowerCase().includes(q)));

  if (!rows.length) { $("tableWrap").innerHTML = '<div class="empty">No records.</div>'; return; }

  const hasActions = ent.canEdit || ent.canDelete || ent.statusFlow;
  const head = ent.columns.map((c) => `<th>${esc(c.label)}</th>`).join("") + (hasActions ? "<th>Actions</th>" : "");
  const body = rows.map((r) => {
    const tds = ent.columns.map((c) => `<td>${cell(r, c)}</td>`).join("");
    return `<tr>${tds}${hasActions ? `<td>${actions(ent, r)}</td>` : ""}</tr>`;
  }).join("");
  $("tableWrap").innerHTML = `<table><thead><tr>${head}</tr></thead><tbody>${body}</tbody></table>`;

  $("tableWrap").querySelectorAll("[data-act]").forEach((btn) => {
    btn.onclick = () => handleAction(btn.dataset.act, btn.dataset.id, btn.dataset.val);
  });
}

function cell(r, c) {
  let v = c.calc ? c.calc(r) : r[c.k];
  if (c.fmt === "rupees") return rupees(v);
  if (c.fmt === "date") return asDate(v);
  if (c.fmt === "bool") return v ? "Yes" : "No";
  if (c.fmt === "status") return `<span class="pill ${statusKind(v)}">${esc(v)}</span>`;
  v = String(v ?? "");
  return esc(v.length > 60 ? v.slice(0, 57) + "…" : v);
}

function actions(ent, r) {
  const id = esc(r[ent.pk]);
  let out = "";
  if (ent.statusFlow) {
    for (const [label, val] of Object.entries(ent.statusFlow)) {
      if (String(r.status).toUpperCase() === val) continue;
      out += `<button class="link" data-act="status" data-id="${id}" data-val="${val}">${label}</button>`;
    }
  }
  if (ent.canEdit) out += `<button class="link" data-act="edit" data-id="${id}">Edit</button>`;
  if (ent.canDelete) out += `<button class="link" data-act="delete" data-id="${id}">Delete</button>`;
  return `<div class="row-actions">${out}</div>`;
}

async function handleAction(act, id, val) {
  const ent = ENTITIES[currentKey];
  const row = currentRows.find((r) => String(r[ent.pk]) === String(id));
  if (act === "edit") return openModal(row);
  if (act === "delete") {
    if (!confirm("Delete this record?")) return;
    const { error } = await sb.from(ent.table).delete().eq(ent.pk, id);
    return finish(error, "Deleted");
  }
  if (act === "status") {
    const patch = { status: val };
    if (ent.commentField) {
      const c = prompt("Note for the employee (optional):", row?.[ent.commentField] || "");
      if (c === null) return; // cancelled
      patch[ent.commentField] = c;
    }
    const { error } = await sb.from(ent.table).update(patch).eq(ent.pk, id);
    return finish(error, `Marked ${val}`);
  }
}

// ---------- modal (add / edit) ----------
let modalRow = null;
function openModal(row) {
  const ent = ENTITIES[currentKey];
  modalRow = row;
  $("modalTitle").textContent = (row ? "Edit " : "Add ") + ent.title.replace(/s$/, "");
  const form = $("modalForm");
  form.innerHTML = ent.fields.map((f) => {
    const v = row ? (row[f.k] ?? "") : "";
    if (f.type === "textarea") return `<label>${esc(f.label)}<textarea data-k="${f.k}" rows="3">${esc(v)}</textarea></label>`;
    if (f.type === "checkbox") return `<label style="flex-direction:row;align-items:center;gap:8px"><input type="checkbox" data-k="${f.k}" ${v ? "checked" : ""}/> ${esc(f.label)}</label>`;
    const t = f.type === "number" ? "number" : "text";
    return `<label>${esc(f.label)}<input type="${t}" data-k="${f.k}" value="${esc(v)}"/></label>`;
  }).join("");
  $("modal").classList.remove("hidden");
}
$("modalCancel").addEventListener("click", () => $("modal").classList.add("hidden"));
$("modalSave").addEventListener("click", saveModal);

async function saveModal() {
  const ent = ENTITIES[currentKey];
  const patch = {};
  $("modalForm").querySelectorAll("[data-k]").forEach((el) => {
    const f = ent.fields.find((x) => x.k === el.dataset.k);
    patch[f.k] = f.type === "number" ? Number(el.value || 0) : f.type === "checkbox" ? el.checked : el.value;
  });
  let error;
  if (modalRow) {
    ({ error } = await sb.from(ent.table).update(patch).eq(ent.pk, modalRow[ent.pk]));
  } else {
    if (ent.idPrefix && !patch[ent.pk]) patch[ent.pk] = ent.idPrefix + Date.now();
    ({ error } = await sb.from(ent.table).insert(patch));
  }
  if (!error) $("modal").classList.add("hidden");
  finish(error, modalRow ? "Saved" : "Added");
}

// ---------- feedback ----------
function finish(error, okMsg) {
  const b = $("banner");
  b.classList.remove("hidden", "ok", "err");
  if (error) { b.classList.add("err"); b.textContent = error.message; }
  else { b.classList.add("ok"); b.textContent = okMsg; load(); }
  setTimeout(() => b.classList.add("hidden"), 3500);
}

boot();
