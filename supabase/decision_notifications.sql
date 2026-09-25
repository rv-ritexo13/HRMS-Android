-- =====================================================================
-- Decision notifications: when an admin approves/rejects a leave, expense
-- or attendance correction, notify the employee.
--   1) inserts an in-app row into public.notifications (shown in the app)
--   2) fires an async HTTP call (pg_net) to the `send-email` Edge Function
--
-- Idempotent: safe to re-run. Run this in the Supabase SQL editor once.
--
-- SETUP:
--   * Deploy the `send-email` Edge Function first, with "Verify JWT" OFF
--     (the DB calls it with a shared-secret header, not a user JWT).
--   * Set these Edge Function secrets: RESEND_API_KEY, MAIL_FROM,
--     and NOTIFY_SECRET.
--   * Replace <NOTIFY_SECRET> below with the SAME value as the NOTIFY_SECRET
--     function secret, then run this script.
--
-- Why the secret is inlined here (not a DB setting): Supabase's hosted SQL
-- role can't run `alter database ... set` (ERROR 42501). The secret lives
-- only inside the database and is sent only over HTTPS to our own function,
-- so inlining it in this security-definer function is safe. Do NOT commit
-- the real value — keep the <NOTIFY_SECRET> placeholder in version control.
-- =====================================================================

create extension if not exists pg_net;

create or replace function public.notify_request_decision()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  kind  text := tg_argv[0];                 -- 'Leave' | 'Expense' | 'Correction'
  title text;
  body  text;
begin
  -- only fire on a real transition into a terminal decision
  if new.status is not distinct from old.status then return new; end if;
  if new.status not in ('APPROVED', 'REJECTED') then return new; end if;
  if new.user_id is null then return new; end if;

  title := kind || ' ' || initcap(lower(new.status));
  body  := 'Your ' || lower(kind) || ' request has been ' || lower(new.status) || '.';

  -- 1) in-app notification. Negative epoch as sort_order so the newest row
  --    sorts first (the app fetches `order=sort_order.asc`).
  insert into public.notifications (id, title, body, time_label, user_id, sort_order, is_read)
  values (
    kind || '-' || new.id || '-' || new.status,
    title,
    body,
    to_char(now(), 'DD Mon'),
    new.user_id,
    -extract(epoch from now())::int,
    false
  )
  on conflict (id) do nothing;

  -- 2) email (async; never blocks or fails the admin's action)
  perform net.http_post(
    url     := 'https://fqkofbenaczxgqkjrrvz.functions.supabase.co/send-email',
    headers := jsonb_build_object(
                 'Content-Type',    'application/json',
                 'x-notify-secret', '<NOTIFY_SECRET>'
               ),
    body    := jsonb_build_object(
                 'user_id', new.user_id,
                 'kind',    kind,
                 'status',  new.status,
                 'title',   title,
                 'body',    body
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
