-- Indeksen brukte feil status (status = AVKLARES)
drop index if exists uidx_bosatt_periode_avklaring_referanse_avklares;

alter table bosatt_periode_avklaring
    add column if not exists behandlingId bigint;

-- Periodeavklaringer under arbeid tillater kun unike referanser innenfor en holder.
-- Holder er lagt til for å gjøre det mindre strengt. F.eks tillate å lage kopi av grunnlag med periodeavklaring under arbeid
create unique index uidx_bosatt_periode_avklaring_referanse_avklares
    on bosatt_periode_avklaring (bosatt_avklaring_holder_id, referanse) where status = 'UNDER_ARBEID';
