// Supabase Edge Function: send-email
// Called by the notify_request_decision() DB trigger (via pg_net) when an admin
// approves/rejects a leave, expense or correction. Sends the employee an email
// via Resend. Not called by clients — it is gated by a shared secret header
// (x-notify-secret), and uses the service_role key to resolve the recipient.
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("method not allowed", { status: 405 });

  // Only the DB trigger (which knows NOTIFY_SECRET) may call this.
  if (req.headers.get("x-notify-secret") !== Deno.env.get("NOTIFY_SECRET")) {
    return new Response("unauthorized", { status: 401 });
  }

  try {
    const { user_id, title, body, kind, status } = await req.json();
    if (!user_id) return new Response("user_id required", { status: 400 });

    const admin = createClient(
      Deno.env.get("SUPABASE_URL")!,
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    );

    // Recipient: prefer a real work_email, else fall back to the auth email.
    let email: string | undefined;
    let name = "there";
    const { data: prof } = await admin
      .from("profiles")
      .select("work_email, full_name")
      .eq("user_id", user_id)
      .maybeSingle();
    if (prof?.full_name) name = prof.full_name;
    if (prof?.work_email && prof.work_email.includes("@")) email = prof.work_email;
    if (!email) {
      const { data } = await admin.auth.admin.getUserById(user_id);
      email = data.user?.email ?? undefined;
    }
    if (!email) return new Response(JSON.stringify({ skipped: "no email" }), { status: 200 });

    const approved = String(status).toUpperCase() === "APPROVED";
    const accent = approved ? "#16a34a" : "#dc2626";
    const html = `
      <div style="font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:480px;margin:auto">
        <h2 style="margin:0 0 4px">WorkNexa</h2>
        <p style="color:#555;margin:0 0 20px">${kind} request update</p>
        <p>Hi ${name},</p>
        <p>${body}</p>
        <p style="display:inline-block;padding:6px 14px;border-radius:8px;color:#fff;background:${accent};font-weight:600">
          ${String(status).toUpperCase()}
        </p>
        <p style="color:#888;font-size:12px;margin-top:24px">
          Open the WorkNexa app to view the details.
        </p>
      </div>`;

    const res = await fetch("https://api.resend.com/emails", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${Deno.env.get("RESEND_API_KEY")}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        // NOTE: use a sender on a domain verified in Resend, else delivery fails.
        from: Deno.env.get("MAIL_FROM") ?? "WorkNexa <onboarding@resend.dev>",
        to: email,
        reply_to: Deno.env.get("MAIL_REPLY_TO") ?? "vsgowda007@gmail.com",
        subject: title,
        html,
      }),
    });

    if (!res.ok) {
      const err = await res.text();
      return new Response(JSON.stringify({ error: err }), { status: 502 });
    }
    return new Response(JSON.stringify({ ok: true }), { status: 200 });
  } catch (e) {
    return new Response(JSON.stringify({ error: String(e) }), { status: 500 });
  }
});
