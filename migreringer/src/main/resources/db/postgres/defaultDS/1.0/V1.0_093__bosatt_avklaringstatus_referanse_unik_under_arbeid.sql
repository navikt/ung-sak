alter table bosatt_periode_avklaring
    add column if not exists status varchar(50) default 'UNDER_ARBEID' not null;

-- Fjernes fordi avklaring og varsling kun utføres når vilkåret er foreslått ikke-oppfylt. Dette feltet er derfor alltid false.
alter table bosatt_periode_avklaring
    drop column if exists er_bosatt_i_trondheim;


-- Referanse kopieres til nytt grunnlag, og følger periodeavklaringen i senere behandlinger.
-- Hvis deler av periodeavklaringen senere overskrives, slik at avklaringen splittes i to segmenter, bør begge segmentene ha samme grunnlagsreferanse for sporbarhet mot brukers varsel.
-- Eks: Behandling 1: | R1 |
--      Behandling 2: | R1 | R2 | R1 |
-- Referanse kan derfor ikke være unik, og heller ikke unik innenfor en holder/behandling.
alter table bosatt_periode_avklaring
    drop constraint if exists bosatt_periode_avklaring_referanse_key;

-- Periodeavklaringer under arbeid tillater kun unike referanser innenfor en holder.
-- Holder er lagt til for å gjøre det mindre strengt. F.eks tillate å lage kopi av grunnlag med periodeavklaring under arbeid
create unique index uidx_bosatt_periode_avklaring_referanse_avklares
    on bosatt_periode_avklaring (bosatt_avklaring_holder_id, referanse) where status = 'AVKLARES';

create index idx_bosatt_periode_avklaring_referanse
    on bosatt_periode_avklaring (referanse);
