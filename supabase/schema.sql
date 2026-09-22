-- HRMS backend — complete provisioning for Supabase (Postgres + PostgREST + GoTrue).
--
-- HOW TO RUN: Supabase dashboard -> SQL Editor -> paste this whole file -> Run.
-- Safe to re-run: every object uses "if not exists" / "on conflict do nothing" /
-- "drop policy if exists".
--
-- This provisions the FULL backend the Android app + web admin dashboard expect:
--   * all data tables (money = whole-rupee bigint; app epoch-millis dates = bigint)
--   * two demo auth users (admin + employee) created directly, email-confirmed
--   * an admins table + is_admin() so admins bypass per-user scoping
--   * per-row user_id ownership + Row Level Security:
--       - per-user tables: visible to their owner OR an admin
--       - global tables (holidays, announcements): read by any signed-in user,
--         written only by admins
--       - an anonymous caller (anon key, not logged in) sees nothing
--
-- Demo logins:  admin@hrms.app / admin123456   (web admin dashboard)
--               emp001@hrms.app / 123456        (mobile app; EMP001 maps to this)
--
-- SECURITY: only the anon (publishable) key belongs in clients. Never ship the
-- service_role key.

create extension if not exists pgcrypto with schema extensions;

-- Seed helper: epoch millis from a date literal.
create or replace function public.millis(d date)
returns bigint language sql immutable as $$
  select (extract(epoch from d) * 1000)::bigint
$$;

-- Fixed demo user UUIDs (referenced by seed rows below).
--   admin: a0000000-0000-4000-8000-000000000001
--   emp:   e0000000-0000-4000-8000-000000000001

-- =====================================================================
-- Auth users (created directly, email-confirmed). Token columns are set to ''
-- (not NULL) or GoTrue's row scan fails with "Database error querying schema".
-- auth.identities.email is a generated column, so it is not inserted.
-- =====================================================================
insert into auth.users (id, instance_id, aud, role, email, encrypted_password, email_confirmed_at,
  raw_app_meta_data, raw_user_meta_data, created_at, updated_at, is_sso_user, is_anonymous,
  confirmation_token, recovery_token, email_change_token_new, email_change,
  email_change_token_current, phone_change, phone_change_token, reauthentication_token)
values
 ('a0000000-0000-4000-8000-000000000001','00000000-0000-0000-0000-000000000000','authenticated','authenticated',
   'admin@hrms.app', extensions.crypt('admin123456', extensions.gen_salt('bf')), now(),
   '{"provider":"email","providers":["email"]}','{"role":"admin","name":"HR Admin"}', now(), now(), false, false,
   '','','','','','','',''),
 ('e0000000-0000-4000-8000-000000000001','00000000-0000-0000-0000-000000000000','authenticated','authenticated',
   'emp001@hrms.app', extensions.crypt('123456', extensions.gen_salt('bf')), now(),
   '{"provider":"email","providers":["email"]}','{"employee_id":"EMP001","name":"Rahul Verma"}', now(), now(), false, false,
   '','','','','','','','')
on conflict do nothing;

insert into auth.identities (provider_id, user_id, identity_data, provider, last_sign_in_at, created_at, updated_at)
values
 ('a0000000-0000-4000-8000-000000000001','a0000000-0000-4000-8000-000000000001',
   jsonb_build_object('sub','a0000000-0000-4000-8000-000000000001','email','admin@hrms.app','email_verified',true),'email', now(), now(), now()),
 ('e0000000-0000-4000-8000-000000000001','e0000000-0000-4000-8000-000000000001',
   jsonb_build_object('sub','e0000000-0000-4000-8000-000000000001','email','emp001@hrms.app','email_verified',true),'email', now(), now(), now())
on conflict do nothing;

-- =====================================================================
-- Admin role infrastructure
-- =====================================================================
create table if not exists public.admins (
    user_id uuid primary key references auth.users(id) on delete cascade
);
alter table public.admins enable row level security;
drop policy if exists admins_self on public.admins;
create policy admins_self on public.admins for select to authenticated using (user_id = auth.uid());
insert into public.admins(user_id) values ('a0000000-0000-4000-8000-000000000001') on conflict do nothing;

create or replace function public.is_admin() returns boolean
  language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.admins a where a.user_id = auth.uid());
$$;

-- =====================================================================
-- Data tables (per-user tables carry user_id defaulting to auth.uid())
-- =====================================================================
create table if not exists public.profiles (
    employee_id            text primary key,
    full_name              text not null,
    department             text not null,
    designation            text not null,
    reporting_manager      text not null,
    joining_date_millis    bigint not null,
    employment_type        text not null,
    dob_millis             bigint not null,
    gender                 text not null,
    phone                  text not null,
    work_email             text not null,
    office_location        text not null,
    emergency_name         text not null,
    emergency_relationship text not null,
    emergency_phone        text not null,
    user_id                uuid references auth.users(id) on delete cascade default auth.uid()
);

create table if not exists public.payslips (
    month_key          text primary key,
    year               int not null,
    month              int not null,
    employee_id        text not null,
    employee_name      text not null,
    department         text not null,
    designation        text not null,
    basic              bigint not null,
    hra                bigint not null,
    special_allowance  bigint not null,
    other_allowances   bigint not null,
    provident_fund     bigint not null,
    professional_tax   bigint not null,
    other_deductions   bigint not null,
    credit_date_millis bigint not null,
    credited           int not null default 1,
    user_id            uuid references auth.users(id) on delete cascade default auth.uid()
);

create table if not exists public.leave_balances (
    leave_type text not null,
    total_days int not null,
    used_days  int not null,
    user_id    uuid references auth.users(id) on delete cascade default auth.uid(),
    primary key (leave_type, user_id)
);

create table if not exists public.leave_requests (
    id               text primary key,
    leave_type       text not null,
    start_millis     bigint not null,
    end_millis       bigint not null,
    days             int not null,
    reason           text not null,
    status           text not null,
    applied_millis   bigint not null,
    manager_comments text not null default '',
    attachment_name  text,
    user_id          uuid references auth.users(id) on delete cascade default auth.uid()
);

create table if not exists public.expenses (
    id                  text primary key,
    title               text not null,
    category            text not null,
    amount              bigint not null,
    expense_date_millis bigint not null,
    description         text not null default '',
    status              text not null,
    created_millis      bigint not null,
    receipt_name        text,
    user_id             uuid references auth.users(id) on delete cascade default auth.uid()
);

create table if not exists public.performance_reviews (
    id            text primary key,
    cycle_name    text not null,
    period_label  text not null,
    reviewer_name text not null,
    rating_label  text,
    status        text not null,
    sort_order    int not null default 0,
    user_id       uuid references auth.users(id) on delete cascade default auth.uid()
);

create table if not exists public.goals (
    id               text primary key,
    title            text not null,
    description      text not null,
    progress_percent int not null,
    due_label        text not null,
    status           text not null,
    sort_order       int not null default 0,
    user_id          uuid references auth.users(id) on delete cascade default auth.uid()
);

create table if not exists public.kras (
    id                text primary key,
    title             text not null,
    weightage_percent int not null,
    target_label      text not null,
    achievement_label text not null,
    rating_label      text not null,
    sort_order        int not null default 0,
    user_id           uuid references auth.users(id) on delete cascade default auth.uid()
);

create table if not exists public.documents (
    id          text primary key,
    category    text not null,
    name        text not null,
    file_type   text not null,
    date_millis bigint not null,
    size_bytes  bigint not null,
    sort_order  int not null default 0,
    user_id     uuid references auth.users(id) on delete cascade default auth.uid()
);

create table if not exists public.notifications (
    id         text primary key,
    title      text not null,
    body       text not null,
    time_label text not null,
    is_read    boolean not null default false,
    sort_order int not null default 0,
    user_id    uuid references auth.users(id) on delete cascade default auth.uid()
);

-- Global (reference) tables — no per-user ownership.
create table if not exists public.holidays (
    id           text primary key,
    day_number   text not null,
    month_abbrev text not null,
    name         text not null,
    day_of_week  text not null,
    sort_order   int not null default 0
);

create table if not exists public.announcements (
    id         text primary key,
    title      text not null,
    body       text not null,
    time_label text not null,
    sort_order int not null default 0
);

-- =====================================================================
-- Row Level Security
-- =====================================================================
do $$
declare t text;
begin
  -- per-user tables: owner or admin
  foreach t in array array['profiles','payslips','leave_balances','leave_requests','expenses',
    'performance_reviews','goals','kras','documents','notifications'] loop
    execute format('alter table public.%I enable row level security;', t);
    execute format('drop policy if exists %I on public.%I;', t||'_owner_admin', t);
    execute format($f$create policy %I on public.%I for all to authenticated
        using (public.is_admin() or user_id = auth.uid())
        with check (public.is_admin() or user_id = auth.uid());$f$, t||'_owner_admin', t);
  end loop;
  -- global tables: any signed-in user reads, only admins write
  foreach t in array array['holidays','announcements'] loop
    execute format('alter table public.%I enable row level security;', t);
    execute format('drop policy if exists %I on public.%I;', t||'_read', t);
    execute format('drop policy if exists %I on public.%I;', t||'_admin_write', t);
    execute format('create policy %I on public.%I for select to authenticated using (true);', t||'_read', t);
    execute format($f$create policy %I on public.%I for all to authenticated
        using (public.is_admin()) with check (public.is_admin());$f$, t||'_admin_write', t);
  end loop;
end $$;

-- =====================================================================
-- Seed data (owned by the demo employee EMP001)
-- =====================================================================
insert into public.profiles values (
    'EMP001','Rahul Verma','Engineering','Software Engineer','Vikram Rao',
    public.millis(date '2022-06-13'),'Full-time',public.millis(date '1996-03-08'),
    'Male','+91 98765 43210','rahul.verma@riteox.co.in','Bengaluru - Prestige Tech Park',
    'Sunita Verma','Mother','+91 98111 22334','e0000000-0000-4000-8000-000000000001'
) on conflict (employee_id) do nothing;

insert into public.leave_balances (leave_type,total_days,used_days,user_id) values
    ('CASUAL',12,4,'e0000000-0000-4000-8000-000000000001'),
    ('SICK',12,2,'e0000000-0000-4000-8000-000000000001'),
    ('EARNED',18,4,'e0000000-0000-4000-8000-000000000001'),
    ('WORK_FROM_HOME',8,3,'e0000000-0000-4000-8000-000000000001'),
    ('OPTIONAL_HOLIDAY',4,1,'e0000000-0000-4000-8000-000000000001')
on conflict do nothing;

insert into public.leave_requests (id,leave_type,start_millis,end_millis,days,reason,status,applied_millis,manager_comments,attachment_name,user_id) values
    ('LR-seed-1','EARNED',public.millis(date '2026-08-10'),public.millis(date '2026-08-12'),3,'Family function at hometown.','APPROVED',public.millis(date '2026-08-01'),'Approved. Enjoy your time off!',null,'e0000000-0000-4000-8000-000000000001'),
    ('LR-seed-2','SICK',public.millis(date '2026-09-05'),public.millis(date '2026-09-05'),1,'Down with fever.','APPROVED',public.millis(date '2026-09-05'),'Get well soon.',null,'e0000000-0000-4000-8000-000000000001'),
    ('LR-seed-3','CASUAL',public.millis(date '2026-09-22'),public.millis(date '2026-09-23'),2,'Personal work.','PENDING',public.millis(date '2026-09-18'),'',null,'e0000000-0000-4000-8000-000000000001'),
    ('LR-seed-4','WORK_FROM_HOME',public.millis(date '2026-07-15'),public.millis(date '2026-07-15'),1,'Home maintenance visit.','REJECTED',public.millis(date '2026-07-14'),'Team on-site day; please reschedule.',null,'e0000000-0000-4000-8000-000000000001'),
    ('LR-seed-5','CASUAL',public.millis(date '2026-06-02'),public.millis(date '2026-06-02'),1,'Ran an errand, no longer needed.','CANCELLED',public.millis(date '2026-06-01'),'',null,'e0000000-0000-4000-8000-000000000001')
on conflict (id) do nothing;

insert into public.expenses (id,title,category,amount,expense_date_millis,description,status,created_millis,receipt_name,user_id) values
    ('EXP-seed-1','Client visit cab fare','TRANSPORTATION',850,public.millis(date '2026-09-12'),'Round trip to client office for the quarterly review.','PAID',public.millis(date '2026-09-12'),'cab_receipt.pdf','e0000000-0000-4000-8000-000000000001'),
    ('EXP-seed-2','Team lunch','FOOD',2400,public.millis(date '2026-09-08'),'Sprint closing lunch with the team (6 people).','APPROVED',public.millis(date '2026-09-08'),'lunch_bill.jpg','e0000000-0000-4000-8000-000000000001'),
    ('EXP-seed-3','Hotel stay - Mumbai offsite','ACCOMMODATION',7200,public.millis(date '2026-09-03'),'Two nights during the Mumbai product offsite.','SUBMITTED',public.millis(date '2026-09-04'),'hotel_invoice.pdf','e0000000-0000-4000-8000-000000000001'),
    ('EXP-seed-4','Flight to Mumbai','TRAVEL',6500,public.millis(date '2026-09-02'),'Return air travel for the offsite.','REJECTED',public.millis(date '2026-09-02'),null,'e0000000-0000-4000-8000-000000000001'),
    ('EXP-seed-5','Standing desk riser','OFFICE',3200,public.millis(date '2026-09-18'),'Ergonomic desk riser for the home workstation.','DRAFT',public.millis(date '2026-09-18'),null,'e0000000-0000-4000-8000-000000000001')
on conflict (id) do nothing;

insert into public.performance_reviews (id,cycle_name,period_label,reviewer_name,rating_label,status,sort_order,user_id) values
    ('rev-h1-2026','H1 2026 Appraisal','Jan 2026 - Jun 2026','Anita Deshmukh','4.3 / 5','COMPLETED',1,'e0000000-0000-4000-8000-000000000001'),
    ('rev-h2-2026','H2 2026 Appraisal','Jul 2026 - Dec 2026','Anita Deshmukh',null,'IN_PROGRESS',2,'e0000000-0000-4000-8000-000000000001'),
    ('rev-annual-2026','Annual Review 2026','Dec 2026','Rohit Nair',null,'UPCOMING',3,'e0000000-0000-4000-8000-000000000001')
on conflict (id) do nothing;

insert into public.goals (id,title,description,progress_percent,due_label,status,sort_order,user_id) values
    ('goal-1','Ship attendance module v2','Deliver geofenced check-in and correction workflow to production.',80,'31 Oct 2026','ON_TRACK',1,'e0000000-0000-4000-8000-000000000001'),
    ('goal-2','Reduce crash-free rate gap','Bring the app crash-free sessions rate above 99.5%.',45,'30 Nov 2026','AT_RISK',2,'e0000000-0000-4000-8000-000000000001'),
    ('goal-3','Mentor two junior engineers','Run weekly pairing and code-review sessions through the half.',100,'30 Jun 2026','COMPLETED',3,'e0000000-0000-4000-8000-000000000001')
on conflict (id) do nothing;

insert into public.kras (id,title,weightage_percent,target_label,achievement_label,rating_label,sort_order,user_id) values
    ('kra-1','Delivery & Quality',40,'Ship 4 modules on schedule','3 of 4 shipped','4.0 / 5',1,'e0000000-0000-4000-8000-000000000001'),
    ('kra-2','Code Quality & Reviews',25,'Review SLA under 24h','Avg 18h turnaround','4.5 / 5',2,'e0000000-0000-4000-8000-000000000001'),
    ('kra-3','Collaboration & Ownership',20,'Lead 2 cross-team initiatives','2 led','4.2 / 5',3,'e0000000-0000-4000-8000-000000000001'),
    ('kra-4','Learning & Growth',15,'Complete 2 certifications','1 completed','3.5 / 5',4,'e0000000-0000-4000-8000-000000000001')
on conflict (id) do nothing;

insert into public.documents (id,category,name,file_type,date_millis,size_bytes,sort_order,user_id) values
    ('payslip-2026-09','PAYSLIPS','Payslip - September 2026','PDF',public.millis(date '2026-09-30'),98446,1,'e0000000-0000-4000-8000-000000000001'),
    ('payslip-2026-08','PAYSLIPS','Payslip - August 2026','PDF',public.millis(date '2026-08-31'),97210,2,'e0000000-0000-4000-8000-000000000001'),
    ('offer-letter','OFFER_LETTER','Offer Letter','PDF',public.millis(date '2022-05-20'),184320,3,'e0000000-0000-4000-8000-000000000001'),
    ('appointment-letter','APPOINTMENT_LETTER','Appointment Letter','PDF',public.millis(date '2022-06-13'),176128,4,'e0000000-0000-4000-8000-000000000001'),
    ('experience-letter','EXPERIENCE_LETTER','Experience Letter','PDF',public.millis(date '2025-04-01'),152064,5,'e0000000-0000-4000-8000-000000000001'),
    ('form-16-fy2425','TAX_DOCUMENTS','Form 16 - FY 2024-25','PDF',public.millis(date '2025-06-15'),210944,6,'e0000000-0000-4000-8000-000000000001'),
    ('investment-proof','TAX_DOCUMENTS','Investment Proof Acknowledgement','JPG',public.millis(date '2025-01-10'),512000,7,'e0000000-0000-4000-8000-000000000001'),
    ('code-of-conduct','COMPANY_POLICIES','Code of Conduct','PDF',public.millis(date '2024-01-05'),245760,8,'e0000000-0000-4000-8000-000000000001'),
    ('leave-policy','COMPANY_POLICIES','Leave Policy','PDF',public.millis(date '2024-01-05'),133120,9,'e0000000-0000-4000-8000-000000000001'),
    ('id-card-scan','OTHER','ID Card Scan','PNG',public.millis(date '2022-06-14'),384000,10,'e0000000-0000-4000-8000-000000000001')
on conflict (id) do nothing;

insert into public.notifications (id,title,body,time_label,is_read,sort_order,user_id) values
    ('NOTIF-1','Leave request approved','Your leave for Oct 2 has been approved by your manager.','1h ago',false,1,'e0000000-0000-4000-8000-000000000001'),
    ('NOTIF-2','Payslip generated','Your September payslip is ready to view.','1 day ago',true,2,'e0000000-0000-4000-8000-000000000001'),
    ('NOTIF-3','Policy update','The work-from-home policy has been updated. Please review it.','3 days ago',true,3,'e0000000-0000-4000-8000-000000000001')
on conflict (id) do nothing;

insert into public.holidays values
    ('HOL-1','01','JAN','New Year''s Day','Thursday',1),
    ('HOL-2','26','JAN','Republic Day','Monday',2),
    ('HOL-3','04','MAR','Holi','Wednesday',3),
    ('HOL-4','03','APR','Good Friday','Friday',4),
    ('HOL-5','14','APR','Ambedkar Jayanti','Tuesday',5),
    ('HOL-6','01','MAY','May Day','Friday',6),
    ('HOL-7','15','AUG','Independence Day','Saturday',7),
    ('HOL-8','02','OCT','Gandhi Jayanti','Friday',8),
    ('HOL-9','20','OCT','Dussehra','Tuesday',9),
    ('HOL-10','08','NOV','Diwali','Sunday',10),
    ('HOL-11','25','DEC','Christmas','Friday',11)
on conflict (id) do nothing;

insert into public.announcements values
    ('ANN-1','Q3 town hall scheduled','Join the all-hands session to hear about this quarter''s goals and wins.','2 days ago',1),
    ('ANN-2','New wellness benefits rolled out','Expanded health coverage and a wellness stipend are now active for all employees.','5 days ago',2),
    ('ANN-3','Office WiFi maintenance this weekend','Expect brief connectivity drops on Saturday between 10 PM and midnight.','1 week ago',3)
on conflict (id) do nothing;

-- Payslips: 8 months ending Sep 2026.
insert into public.payslips (month_key,year,month,employee_id,employee_name,department,designation,basic,hra,special_allowance,other_allowances,provident_fund,professional_tax,other_deductions,credit_date_millis,credited,user_id) values
    ('2026-09',2026,9,'EMP001','Rahul Verma','Engineering','Software Engineer',42500,17000,21250,4250,5100,200,700,public.millis(date '2026-09-30'),1,'e0000000-0000-4000-8000-000000000001'),
    ('2026-08',2026,8,'EMP001','Rahul Verma','Engineering','Software Engineer',42500,17000,21250,4250,5100,200,700,public.millis(date '2026-08-31'),1,'e0000000-0000-4000-8000-000000000001'),
    ('2026-07',2026,7,'EMP001','Rahul Verma','Engineering','Software Engineer',41750,16700,20875,4175,5100,200,700,public.millis(date '2026-07-31'),1,'e0000000-0000-4000-8000-000000000001'),
    ('2026-06',2026,6,'EMP001','Rahul Verma','Engineering','Software Engineer',41750,16700,20875,4175,5100,200,700,public.millis(date '2026-06-30'),1,'e0000000-0000-4000-8000-000000000001'),
    ('2026-05',2026,5,'EMP001','Rahul Verma','Engineering','Software Engineer',41750,16700,20875,4175,5100,200,700,public.millis(date '2026-05-31'),1,'e0000000-0000-4000-8000-000000000001'),
    ('2026-04',2026,4,'EMP001','Rahul Verma','Engineering','Software Engineer',41000,16400,20500,4100,5100,200,700,public.millis(date '2026-04-30'),1,'e0000000-0000-4000-8000-000000000001'),
    ('2026-03',2026,3,'EMP001','Rahul Verma','Engineering','Software Engineer',41000,16400,20500,4100,5100,200,700,public.millis(date '2026-03-31'),1,'e0000000-0000-4000-8000-000000000001'),
    ('2026-02',2026,2,'EMP001','Rahul Verma','Engineering','Software Engineer',40250,16100,20125,4025,5100,200,700,public.millis(date '2026-02-28'),1,'e0000000-0000-4000-8000-000000000001')
on conflict (month_key) do nothing;
