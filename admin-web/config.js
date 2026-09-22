// Supabase connection for the HRMS admin dashboard.
// The anon key is safe to ship in a browser client — Row Level Security + the
// is_admin() gate protect the data. NEVER put the service_role key here.
window.HRMS_CONFIG = {
  SUPABASE_URL: "https://fqkofbenaczxgqkjrrvz.supabase.co",
  SUPABASE_ANON_KEY:
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImZxa29mYmVuYWN6eGdxa2pycnZ6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTAwNTEwMjgsImV4cCI6MjEwNTYyNzAyOH0.RcZR4A0ra4K4BMuCQZQ10L801LlxA-FoxHzxG5JfZBM",
};
