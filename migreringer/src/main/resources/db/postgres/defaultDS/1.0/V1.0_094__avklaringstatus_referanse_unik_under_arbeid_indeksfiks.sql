-- Indeksen brukte feil status (status = AVKLARES)
drop index if exists uidx_bosatt_periode_avklaring_referanse_avklares;

alter table bosatt_periode_avklaring
    add column if not exists behandlingId bigint;

-- Backfiller behandlingid på bosatt_periode_avklaring basert på sitt bostedsgrunnlag
-- Kun ment for å ikke ødelegge saker i test som kun har én behandling.
update bosatt_periode_avklaring bpa
set behandlingid = gba.behandling_id
from gr_bosatt_avklaring gba
where gba.foreslatt_holder_id = bpa.bosatt_avklaring_holder_id
  and bpa.behandlingid is null;

alter table bosatt_periode_avklaring
    alter column behandlingId set NOT NULL;

-- Periodeavklaringer under arbeid tillater kun unike referanser innenfor en holder.
-- Holder er lagt til for å gjøre det mindre strengt. F.eks tillate å lage kopi av grunnlag med periodeavklaring under arbeid
create unique index uidx_bosatt_periode_avklaring_referanse_avklares
    on bosatt_periode_avklaring (bosatt_avklaring_holder_id, referanse) where status = 'UNDER_ARBEID';
