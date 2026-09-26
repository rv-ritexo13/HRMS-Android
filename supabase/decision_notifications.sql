-- =====================================================================
-- Decision notifications: when an admin approves/rejects a leave, expense
-- or attendance correction, notify the employee.
--   1) inserts an in-app row into public.notifications (shown in the app)
--   2) sends an email via Resend, called directly from Postgres with pg_net
--
-- This is the DIRECT-TO-RESEND version: the DB calls api.resend.com itself,
-- so the send-email Edge Function is NOT required (it was dropped because the
-- Supabase gateway's JWT/apikey handling and dashboard deploys were flaky).
-- The old supabase/functions/send-email/ is kept for reference only.
--
-- Idempotent: safe to re-run. Run this in the Supabase SQL editor once.
--
-- SETUP:
--   * create extension if not exists pg_net;   (included below)
--   * Replace <RESEND_API_KEY> with your Resend key (re_...). It lives only
--     inside the database (security-definer function) and travels only over
--     HTTPS to Resend, so inlining it here is safe. Do NOT commit the real
--     key — keep the <RESEND_API_KEY> placeholder in version control.
--   * Sender: with no verified domain, use 'onboarding@resend.dev', which
--     only delivers to your own Resend account email. For real recipients,
--     verify a domain in Resend and change the 'from' address below.
-- =====================================================================

create extension if not exists pg_net;

create or replace function public.notify_request_decision()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  kind    text := tg_argv[0];                -- 'Leave' | 'Expense' | 'Correction'
  title   text;
  body    text;
  v_email text;
  v_name  text;
  v_html  text;
  v_accent text;
begin
  if new.status is not distinct from old.status then return new; end if;
  if new.status not in ('APPROVED', 'REJECTED') then return new; end if;
  if new.user_id is null then return new; end if;

  title := kind || ' ' || initcap(lower(new.status));
  body  := 'Your ' || lower(kind) || ' request has been ' || lower(new.status) || '.';

  -- 1) in-app notification (negative epoch so the newest row sorts first)
  insert into public.notifications (id, title, body, time_label, user_id, sort_order, is_read)
  values (kind || '-' || new.id || '-' || new.status, title, body,
          to_char(now(), 'DD Mon'), new.user_id, -extract(epoch from now())::int, false)
  on conflict (id) do nothing;

  -- 2) resolve recipient: work_email if it has '@', else the auth login email
  select coalesce(p.full_name, 'there'),
         case when p.work_email like '%@%' then p.work_email else u.email end
    into v_name, v_email
  from auth.users u
  left join public.profiles p on p.user_id = u.id
  where u.id = new.user_id;

  if v_email is null then return new; end if;

  v_accent := case when new.status = 'APPROVED' then '#16a34a' else '#dc2626' end;
  v_html := '<div style="font-family:-apple-system,Segoe UI,Roboto,sans-serif;max-width:480px;margin:auto">'
         || '<h2 style="margin:0 0 4px">WorkNexa</h2>'
         || '<p style="color:#555;margin:0 0 20px">' || kind || ' request update</p>'
         || '<p>Hi ' || v_name || ',</p><p>' || body || '</p>'
         || '<p style="display:inline-block;padding:6px 14px;border-radius:8px;color:#fff;background:'
         || v_accent || ';font-weight:600">' || upper(new.status) || '</p>'
         || '<p style="color:#888;font-size:12px;margin-top:24px">Open the WorkNexa app to view the details.</p>'
         || '</div>';

  -- 3) send via Resend directly
  perform net.http_post(
    url     := 'https://api.resend.com/emails',
    headers := jsonb_build_object(
                 'Content-Type',  'application/json',
                 'Authorization', 'Bearer <RESEND_API_KEY>'
               ),
    body    := jsonb_build_object(
                 'from',     'WorkNexa <onboarding@resend.dev>',
                 'to',       v_email,
                 'reply_to', 'vsgowda007@gmail.com',
                 'subject',  title,
                 'html',     v_html
               )
  );
  return new;
end;
$$;

drop trigger if exists trg_leave_decision       on public.leave_requests;
drop trigger if exists trg_expense_decision     on public.expenses;
drop trigger if exists trg_correction_decision  on public.attendance_corrections;

create trigger trg_leave_decision
  after update of status on public.leave_requests
  for each row execute function public.notify_request_decision('Leave');

create trigger trg_expense_decision
  after update of status on public.expenses
  for each row execute function public.notify_request_decision('Expense');

create trigger trg_correction_decision
  after update of status on public.attendance_corrections
  for each row execute function public.notify_request_decision('Correction');
