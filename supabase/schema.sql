-- HRMS backend schema for Supabase (Postgres + PostgREST).
--
-- HOW TO RUN: Supabase dashboard -> SQL Editor -> paste this whole file -> Run.
-- Safe to re-run: every object is created with "if not exists" / "drop policy if exists".
--
-- Money is stored as whole rupees (bigint), matching the Android app's
-- CurrencyUtils.formatRupees(long). Dates that the app keeps as epoch millis are
-- stored as bigint millis so the client mapping stays 1:1 with the current
-- SQLite columns. Reference/content dates use plain date/text.
--
-- SECURITY NOTE: RLS is enabled on every table. For this single-demo-employee
-- build the policies below allow the anon + authenticated roles full access, so
-- the app works immediately with just the anon key. Before real multi-user use,
-- replace the "using (true)" policies with employee-scoped checks against
-- auth.uid() / a profiles.user_id column.

-- =====================================================================
-- Helper: seed epoch-millis from a date literal
-- =====================================================================
create or replace function public.millis(d date)
returns bigint language sql immutable as $$
  select (extract(epoch from d) * 1000)::bigint
$$;

-- =====================================================================
-- profiles
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
    emergency_phone        text not null
);

-- =====================================================================
-- payslips
-- =====================================================================
create table if not exists public.payslips (
    month_key          text primary key,   -- "YYYY-MM"
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
    credited           int not null default 1
);

-- =====================================================================
-- leave
-- =====================================================================
create table if not exists public.leave_balances (
    leave_type text primary key,
    total_days int not null,
    used_days  int not null
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
    attachment_name  text
);

-- =====================================================================
-- expenses
-- =====================================================================
create table if not exists public.expenses (
    id                  text primary key,
    title               text not null,
    category            text not null,
    amount              bigint not null,
    expense_date_millis bigint not null,
    description         text not null default '',
    status              text not null,
    created_millis      bigint not null,
    receipt_name        text
);

-- =====================================================================
-- performance management
-- =====================================================================
create table if not exists public.performance_reviews (
    id           text primary key,
    cycle_name   text not null,
    period_label text not null,
    reviewer_name text not null,
    rating_label text,               -- null while pending
    status       text not null,      -- COMPLETED | IN_PROGRESS | UPCOMING
    sort_order   int not null default 0
);

create table if not exists public.goals (
    id               text primary key,
    title            text not null,
    description      text not null,
    progress_percent int not null,
    due_label        text not null,
    status           text not null,  -- ON_TRACK | AT_RISK | COMPLETED
    sort_order       int not null default 0
);

create table if not exists public.kras (
    id                text primary key,
    title             text not null,
    weightage_percent int not null,
    target_label      text not null,
    achievement_label text not null,
    rating_label      text not null,
    sort_order        int not null default 0
);

-- =====================================================================
-- dashboard content
-- =====================================================================
create table if not exists public.holidays (
    id            text primary key,
    day_number    text not null,   -- "02"
    month_abbrev  text not null,   -- "OCT"
    name          text not null,
    day_of_week   text not null,
    sort_order    int not null default 0
);

create table if not exists public.announcements (
    id         text primary key,
    title      text not null,
    body       text not null,
    time_label text not null,
    sort_order int not null default 0
);

create table if not exists public.notifications (
    id         text primary key,
    title      text not null,
    body       text not null,
    time_label text not null,
    is_read    boolean not null default false,
    sort_order int not null default 0
);

-- =====================================================================
-- Row Level Security: enable + permissive demo policies
-- =====================================================================
do $$
declare t text;
begin
  foreach t in array array[
    'profiles','payslips','leave_balances','leave_requests','expenses',
    'performance_reviews','goals','kras','holidays','announcements','notifications'
  ] loop
    execute format('alter table public.%I enable row level security;', t);
    execute format('drop policy if exists %I on public.%I;', t || '_demo_all', t);
    execute format(
      'create policy %I on public.%I for all to anon, authenticated using (true) with check (true);',
      t || '_demo_all', t);
  end loop;
end $$;

-- =====================================================================
-- Seed data (demo employee EMP001 — Rahul Verma), mirrors the app.
-- =====================================================================
insert into public.profiles values (
    'EMP001','Rahul Verma','Engineering','Software Engineer','Vikram Rao',
    public.millis(date '2022-06-13'),'Full-time',public.millis(date '1996-03-08'),
    'Male','+91 98765 43210','rahul.verma@riteox.co.in','Bengaluru - Prestige Tech Park',
    'Sunita Verma','Mother','+91 98111 22334'
) on conflict (employee_id) do nothing;

insert into public.leave_balances values
    ('CASUAL',12,4),('SICK',12,2),('EARNED',18,4),('WORK_FROM_HOME',8,3),('OPTIONAL_HOLIDAY',4,1)
on conflict (leave_type) do nothing;

insert into public.leave_requests (id,leave_type,start_millis,end_millis,days,reason,status,applied_millis,manager_comments,attachment_name) values
    ('LR-seed-1','EARNED',public.millis(date '2026-08-10'),public.millis(date '2026-08-12'),3,'Family function at hometown.','APPROVED',public.millis(date '2026-08-01'),'Approved. Enjoy your time off!',null),
    ('LR-seed-2','SICK',public.millis(date '2026-09-05'),public.millis(date '2026-09-05'),1,'Down with fever.','APPROVED',public.millis(date '2026-09-05'),'Get well soon.',null),
    ('LR-seed-3','CASUAL',public.millis(date '2026-09-22'),public.millis(date '2026-09-23'),2,'Personal work.','PENDING',public.millis(date '2026-09-18'),'',null),
    ('LR-seed-4','WORK_FROM_HOME',public.millis(date '2026-07-15'),public.millis(date '2026-07-15'),1,'Home maintenance visit.','REJECTED',public.millis(date '2026-07-14'),'Team on-site day; please reschedule.',null),
    ('LR-seed-5','CASUAL',public.millis(date '2026-06-02'),public.millis(date '2026-06-02'),1,'Ran an errand, no longer needed.','CANCELLED',public.millis(date '2026-06-01'),'',null)
on conflict (id) do nothing;

insert into public.expenses (id,title,category,amount,expense_date_millis,description,status,created_millis,receipt_name) values
    ('EXP-seed-1','Client visit cab fare','TRANSPORTATION',850,public.millis(date '2026-09-12'),'Round trip to client office for the quarterly review.','PAID',public.millis(date '2026-09-12'),'cab_receipt.pdf'),
    ('EXP-seed-2','Team lunch','FOOD',2400,public.millis(date '2026-09-08'),'Sprint closing lunch with the team (6 people).','APPROVED',public.millis(date '2026-09-08'),'lunch_bill.jpg'),
    ('EXP-seed-3','Hotel stay - Mumbai offsite','ACCOMMODATION',7200,public.millis(date '2026-09-03'),'Two nights during the Mumbai product offsite.','SUBMITTED',public.millis(date '2026-09-04'),'hotel_invoice.pdf'),
    ('EXP-seed-4','Flight to Mumbai','TRAVEL',6500,public.millis(date '2026-09-02'),'Return air travel for the offsite.','REJECTED',public.millis(date '2026-09-02'),null),
    ('EXP-seed-5','Standing desk riser','OFFICE',3200,public.millis(date '2026-09-18'),'Ergonomic desk riser for the home workstation.','DRAFT',public.millis(date '2026-09-18'),null)
on conflict (id) do nothing;

insert into public.performance_reviews values
    ('rev-h1-2026','H1 2026 Appraisal','Jan 2026 - Jun 2026','Anita Deshmukh','4.3 / 5','COMPLETED',1),
    ('rev-h2-2026','H2 2026 Appraisal','Jul 2026 - Dec 2026','Anita Deshmukh',null,'IN_PROGRESS',2),
    ('rev-annual-2026','Annual Review 2026','Dec 2026','Rohit Nair',null,'UPCOMING',3)
on conflict (id) do nothing;

insert into public.goals values
    ('goal-1','Ship attendance module v2','Deliver geofenced check-in and correction workflow to production.',80,'31 Oct 2026','ON_TRACK',1),
    ('goal-2','Reduce crash-free rate gap','Bring the app crash-free sessions rate above 99.5%.',45,'30 Nov 2026','AT_RISK',2),
    ('goal-3','Mentor two junior engineers','Run weekly pairing and code-review sessions through the half.',100,'30 Jun 2026','COMPLETED',3)
on conflict (id) do nothing;

insert into public.kras values
    ('kra-1','Delivery & Quality',40,'Ship 4 modules on schedule','3 of 4 shipped','4.0 / 5',1),
    ('kra-2','Code Quality & Reviews',25,'Review SLA under 24h','Avg 18h turnaround','4.5 / 5',2),
    ('kra-3','Collaboration & Ownership',20,'Lead 2 cross-team initiatives','2 led','4.2 / 5',3),
    ('kra-4','Learning & Growth',15,'Complete 2 certifications','1 completed','3.5 / 5',4)
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

insert into public.notifications values
    ('NOTIF-1','Leave request approved','Your leave for Oct 2 has been approved by your manager.','1h ago',false,1),
    ('NOTIF-2','Payslip generated','Your September payslip is ready to view.','1 day ago',true,2),
    ('NOTIF-3','Policy update','The work-from-home policy has been updated. Please review it.','3 days ago',true,3)
on conflict (id) do nothing;

-- Payslips: 8 months ending Sep 2026 (gross split 50/20/25 + remainder; deductions fixed at 6,000).
insert into public.payslips (month_key,year,month,employee_id,employee_name,department,designation,basic,hra,special_allowance,other_allowances,provident_fund,professional_tax,other_deductions,credit_date_millis,credited) values
    ('2026-09',2026,9,'EMP001','Rahul Verma','Engineering','Software Engineer',42500,17000,21250,4250,5100,200,700,public.millis(date '2026-09-30'),1),
    ('2026-08',2026,8,'EMP001','Rahul Verma','Engineering','Software Engineer',42500,17000,21250,4250,5100,200,700,public.millis(date '2026-08-31'),1),
    ('2026-07',2026,7,'EMP001','Rahul Verma','Engineering','Software Engineer',41750,16700,20875,4175,5100,200,700,public.millis(date '2026-07-31'),1),
    ('2026-06',2026,6,'EMP001','Rahul Verma','Engineering','Software Engineer',41750,16700,20875,4175,5100,200,700,public.millis(date '2026-06-30'),1),
    ('2026-05',2026,5,'EMP001','Rahul Verma','Engineering','Software Engineer',41750,16700,20875,4175,5100,200,700,public.millis(date '2026-05-31'),1),
    ('2026-04',2026,4,'EMP001','Rahul Verma','Engineering','Software Engineer',41000,16400,20500,4100,5100,200,700,public.millis(date '2026-04-30'),1),
    ('2026-03',2026,3,'EMP001','Rahul Verma','Engineering','Software Engineer',41000,16400,20500,4100,5100,200,700,public.millis(date '2026-03-31'),1),
    ('2026-02',2026,2,'EMP001','Rahul Verma','Engineering','Software Engineer',40250,16100,20125,4025,5100,200,700,public.millis(date '2026-02-28'),1)
on conflict (month_key) do nothing;

-- Done. Verify with:  select count(*) from public.expenses;
