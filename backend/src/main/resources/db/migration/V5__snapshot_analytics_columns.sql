-- Phase 3: pre-aggregated analytics columns for the weekly snapshots table.
-- These feed every chart in the dashboard — queried on page load, never computed live.

ALTER TABLE snapshots
    ADD COLUMN lines_added           INT NOT NULL DEFAULT 0,
    ADD COLUMN lines_deleted         INT NOT NULL DEFAULT 0,
    ADD COLUMN pr_merge_time         INTERVAL,
    ADD COLUMN language_distribution JSONB,
    ADD COLUMN coding_hours          JSONB;
