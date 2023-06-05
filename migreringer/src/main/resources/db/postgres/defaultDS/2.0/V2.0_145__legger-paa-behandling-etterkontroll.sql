alter table Etterkontroll add column if not exists behandling_id bigint references behandling;
-- tillatter flere etterkontroller på samme fagsak.
drop index if exists idx_etterkontroll_1;
create index if not exists idx_etterkontroll_1
    on etterkontroll (fagsak_id);

-- mye brukt condition
create index if not exists idx_etterkontroll_3
    on etterkontroll (behandlet, kontroll_tid);
