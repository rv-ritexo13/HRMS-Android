// Supabase Edge Function: create-employee
// Admin-only. Verifies the caller is an admin (via is_admin() with their JWT),
// then uses the service_role key (auto-injected, never exposed to the client) to
// create the auth user and a matching profile row.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function json(obj: unknown, status = 200) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { ...cors, "Content-Type": "application/json" },
  });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    const authHeader = req.headers.get("Authorization") || "";
    const url = Deno.env.get("SUPABASE_URL")!;
    const anon = Deno.env.get("SUPABASE_ANON_KEY")!;
    const service = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

    // 1) The caller must be an admin — checked with THEIR token via RLS/is_admin().
    const caller = createClient(url, anon, { global: { headers: { Authorization: authHeader } } });
    const { data: isAdmin, error: adminErr } = await caller.rpc("is_admin");
    if (adminErr || isAdmin !== true) return json({ error: "Not authorized" }, 403);

    const b = await req.json();
    const { email, password, employee_id, full_name, department, designation } = b ?? {};
    if (!email || !password || !employee_id || !full_name) {
      return json({ error: "email, password, employee_id and full_name are required" }, 400);
    }

    const admin = createClient(url, service);

    // 2) Create the confirmed auth user.
    const { data: created, error: createErr } = await admin.auth.admin.createUser({
      email,
      password,
      email_confirm: true,
      user_metadata: { employee_id, name: full_name },
    });
    if (createErr) return json({ error: createErr.message }, 400);
    const uid = created.user!.id;

    // 3) Create the matching profile row (owned by the new user).
    const { error: profErr } = await admin.from("profiles").insert({
      employee_id,
      full_name,
      department: department || "General",
      designation: designation || "Employee",
      reporting_manager: "—",
      joining_date_millis: Date.now(),
      employment_type: "Full-time",
      dob_millis: 0,
      gender: "—",
      phone: "—",
      work_email: email,
      office_location: "—",
      emergency_name: "—",
      emergency_relationship: "—",
      emergency_phone: "—",
      user_id: uid,
    });
    if (profErr) return json({ error: "User created but profile insert failed: " + profErr.message }, 500);

    return json({ ok: true, user_id: uid });
  } catch (e) {
    return json({ error: String(e) }, 500);
  }
});
