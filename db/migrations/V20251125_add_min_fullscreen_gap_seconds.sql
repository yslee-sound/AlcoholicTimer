-- Migration: add min_fullscreen_gap_seconds to ad_policy
-- Purpose: 서버에서 전면광고(Interstitial/AppOpen 등) 간 최소 간격을 제어하기 위해 필드 추가
-- Default: 30초 (안전한 기본값)

ALTER TABLE public.ad_policy
ADD COLUMN IF NOT EXISTS min_fullscreen_gap_seconds integer NOT NULL DEFAULT 30;

-- Updated DDL (for reference / new deployments)
--
-- create table public.ad_policy (
--   id bigserial not null,
--   created_at timestamp with time zone null default now(),
--   app_id text not null,
--   is_active boolean not null default true,
--   ad_app_open_enabled boolean not null default true,
--   ad_interstitial_enabled boolean not null default true,
--   ad_banner_enabled boolean not null default true,
--   ad_interstitial_max_per_hour integer not null default 3,
--   ad_interstitial_max_per_day integer not null default 20,
--   app_open_max_per_hour integer not null default 2,
--   app_open_max_per_day integer not null default 15,
--   app_open_cooldown_seconds integer not null default 60,
--   min_fullscreen_gap_seconds integer not null default 30,
--   constraint ad_policy_pkey primary key (id),
--   constraint ad_policy_app_id_key unique (app_id)
-- ) TABLESPACE pg_default;
--
-- create index IF not exists idx_ad_policy_app_id on public.ad_policy using btree (app_id) TABLESPACE pg_default;
-- create index IF not exists idx_ad_policy_is_active on public.ad_policy using btree (is_active) TABLESPACE pg_default;

