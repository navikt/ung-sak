ALTER TABLE behandling
    add column IF NOT EXISTS original_behandling_id bigint references behandling;

