alter table Etterkontroll add column if not exists behandling_id bigint references behandling;
-- tillater flere etterkontroller på samme fagsak.
drop index if exists idx_etterkontroll_1;
create index if not exists idx_etterkontroll_1
    on etterkontroll (fagsak_id);

-- tillater kun én etterkontroll per behandling per kontrolltype
create unique index if not exists idx_etterkontroll_4
    on etterkontroll (behandling_id, kontroll_type)
    where behandlet = false;

-- mye brukt condition
create index if not exists idx_etterkontroll_3
    on etterkontroll (behandlet, kontroll_tid);
