-- gr_bosatt_avklaring blandet to akser i samme rad: avklaringene (som skal over i gr_vilkaar_avklaring) og
-- bostedsinformasjonen fra søknaden.
create sequence seq_gr_bosted_soknadsfakta increment by 50 minvalue 1000000;

create table gr_bosted_soknadsfakta
(
    id                                   bigint primary key,
    behandling_id                        bigint       not null references behandling (id),
    bostedsinformasjon_soeknad_holder_id bigint       not null references bostedsinformasjon_soeknad_holder (id),
    aktiv                                boolean      not null default true,
    versjon                              bigint       not null default 0,
    opprettet_av                         varchar(20)  not null default 'VL',
    opprettet_tid                        timestamp(3) not null default current_timestamp,
    endret_av                            varchar(20),
    endret_tid                           timestamp(3)
);

comment on table gr_bosted_soknadsfakta is
    'Grunnlag som kobler en behandling til bostedsopplysningene oppgitt i søknaden. Én aktiv rad per behandling.';

create unique index uidx_gr_bosted_soknadsfakta_aktiv on gr_bosted_soknadsfakta (behandling_id) where aktiv;
create index idx_gr_bosted_soknadsfakta_behandling on gr_bosted_soknadsfakta (behandling_id);
create index idx_gr_bosted_soknadsfakta_holder on gr_bosted_soknadsfakta (bostedsinformasjon_soeknad_holder_id);

-- Kopierer 1:1 på radnivå, slik at holdere som deles mellom flere grunnlagsversjoner fortsatt deles, og slik at
-- aktiv-flagget bevares (historiske deaktiverte grunnlag skal fortsatt være deaktiverte).
insert into gr_bosted_soknadsfakta (id, behandling_id, bostedsinformasjon_soeknad_holder_id, aktiv, versjon,
                                    opprettet_av, opprettet_tid, endret_av, endret_tid)
select nextval('seq_gr_bosted_soknadsfakta'),
       g.behandling_id,
       g.bostedsinformasjon_soeknad_holder_id,
       g.aktiv,
       g.versjon,
       g.opprettet_av,
       g.opprettet_tid,
       g.endret_av,
       g.endret_tid
from gr_bosatt_avklaring g
where g.bostedsinformasjon_soeknad_holder_id is not null
  and not exists (select 1
                  from gr_bosted_soknadsfakta n
                  where n.behandling_id = g.behandling_id
                    and n.bostedsinformasjon_soeknad_holder_id = g.bostedsinformasjon_soeknad_holder_id
                    and n.opprettet_tid = g.opprettet_tid);


-- Flytte vilkårsavklaringenene for bosted:

-- Ferdigstilt-holder: én ny rad per gammel holder, slik at grunnlag som delte holder fortsatt deler.
create temporary table holder_map
(
    gammel_id bigint primary key,
    ny_id     bigint not null
);

insert into holder_map (gammel_id, ny_id)
select h.id, nextval('seq_vilkaar_avklaring_holder')
from bosatt_avklaring_holder h;

insert into vilkaar_avklaring_holder (id, opprettet_av, opprettet_tid, endret_av, endret_tid)
select m.ny_id, h.opprettet_av, h.opprettet_tid, h.endret_av, h.endret_tid
from bosatt_avklaring_holder h
         join holder_map m on m.gammel_id = h.id;

-- Foreslått-holder: kun for holdere som faktisk har foreslåtte rader. nextval må stå utenfor distinct —
-- «select distinct h.id, nextval(...)» gjør hver rad unik og gir én holder per avklaring.
create temporary table fores_holder_map
(
    gammel_id bigint primary key,
    ny_id     bigint not null
);

insert into fores_holder_map (gammel_id, ny_id)
select h.id, nextval('seq_vilkaar_avklaring_fores_holder')
from (select distinct bosatt_avklaring_holder_id as id from bosatt_periode_avklaring_foreslaatt) h;

insert into vilkaar_avklaring_foreslaatt_holder (id, opprettet_av, opprettet_tid, endret_av, endret_tid)
select m.ny_id, h.opprettet_av, h.opprettet_tid, h.endret_av, h.endret_tid
from bosatt_avklaring_holder h
         join fores_holder_map m on m.gammel_id = h.id;

-- Ferdigstilte avklaringer. skal_sende_varsel coalesce-es fordi kolonnen er nullbar i bosted og NOT NULL i felles.
insert into vilkaar_periode_avklaring (id, vilkaar_avklaring_holder_id, referanse, periode, ikke_oppfylt_aarsak,
                                       begrunnelse, skal_sende_varsel, fritekst_til_varsel, begrunnelse_ikke_varsel,
                                       kilde, kilde_fritekst, avklaringtype, vurdert_av, vurdert_tidspunkt,
                                       opprettet_av, opprettet_tid, endret_av, endret_tid)
select nextval('seq_vilkaar_periode_avklaring'),
       m.ny_id,
       a.referanse,
       a.periode,
       a.ikke_oppfylt_aarsak,
       a.begrunnelse,
       coalesce(a.skal_sende_varsel, false),
       a.fritekst_til_varsel,
       a.begrunnelse_ikke_varsel,
       a.kilde,
       a.kilde_fritekst,
       a.avklaringtype,
       a.vurdert_av,
       a.vurdert_tidspunkt,
       a.opprettet_av,
       a.opprettet_tid,
       a.endret_av,
       a.endret_tid
from bosatt_periode_avklaring a
         join holder_map m on m.gammel_id = a.bosatt_avklaring_holder_id
where not exists (select 1
                  from vilkaar_periode_avklaring v
                           join holder_map hm on hm.ny_id = v.vilkaar_avklaring_holder_id
                  where v.referanse = a.referanse);

insert into vilkaar_periode_avklaring_foreslaatt (id, vilkaar_avklaring_fores_holder_id, referanse, periode,
                                                  ikke_oppfylt_aarsak, begrunnelse, skal_sende_varsel,
                                                  fritekst_til_varsel, begrunnelse_ikke_varsel, kilde, kilde_fritekst,
                                                  avklaringtype, vurdert_av, vurdert_tidspunkt,
                                                  opprettet_av, opprettet_tid, endret_av, endret_tid)
select nextval('seq_vilkaar_periode_avklaring_fores'),
       m.ny_id,
       a.referanse,
       a.periode,
       a.ikke_oppfylt_aarsak,
       a.begrunnelse,
       coalesce(a.skal_sende_varsel, false),
       a.fritekst_til_varsel,
       a.begrunnelse_ikke_varsel,
       a.kilde,
       a.kilde_fritekst,
       a.avklaringtype,
       a.vurdert_av,
       a.vurdert_tidspunkt,
       a.opprettet_av,
       a.opprettet_tid,
       a.endret_av,
       a.endret_tid
from bosatt_periode_avklaring_foreslaatt a
         join fores_holder_map m on m.gammel_id = a.bosatt_avklaring_holder_id
where not exists (select 1
                  from vilkaar_periode_avklaring_foreslaatt v
                           join fores_holder_map hm on hm.ny_id = v.vilkaar_avklaring_fores_holder_id
                  where v.referanse = a.referanse);

-- Grunnlaget henter begge holder-FK-ene fra samme gamle holder-id. avklaring_holder_id er nullbar på
-- gr_bosatt_avklaring (et grunnlag kunne ha søknadsfakta uten avklaring), så begge joinene må være left join.
insert into gr_vilkaar_avklaring (id, behandling_id, vilkaar_type, foreslaatt_holder_id, avklaring_holder_id,
                                  aktiv, versjon, opprettet_av, opprettet_tid, endret_av, endret_tid)
select nextval('seq_gr_vilkaar_avklaring'),
       g.behandling_id,
       'BOSTEDSVILKÅR',
       fm.ny_id,
       hm.ny_id,
       g.aktiv,
       g.versjon,
       g.opprettet_av,
       g.opprettet_tid,
       g.endret_av,
       g.endret_tid
from gr_bosatt_avklaring g
         left join holder_map hm on hm.gammel_id = g.avklaring_holder_id
         left join fores_holder_map fm on fm.gammel_id = g.avklaring_holder_id;

drop table holder_map;
drop table fores_holder_map;
