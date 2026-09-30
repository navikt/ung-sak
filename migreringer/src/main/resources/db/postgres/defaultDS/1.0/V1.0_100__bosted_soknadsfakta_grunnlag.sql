-- gr_bosatt_avklaring blandet to akser i samme rad: avklaringene (som skal over i gr_vilkaar_avklaring) og
-- bostedsinformasjonen fra søknaden. Koblingen bostedsinformasjon_soeknad_holder_id var i praksis påkrevd
-- (nullable = false på entiteten), og er grunnen til at grunnlaget ikke kan gjenbrukes av et vilkår uten
-- søknadsfakta. Her får søknadsfakta sitt eget grunnlag, slik at avklaringsaksen kan flyttes for seg selv.
--
-- bosatt_*-tabellene røres ikke: de står urørt til slettemigreringen, slik at en tilbakerulling er en ren
-- kodetilbakerulling.

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
-- Idempotent på (behandling_id, holder_id, opprettet_tid): kopieringen er ren, og må kunne kjøres om igjen som
-- etterslepsmigrering i neste release for å fange skrivinger som traff den gamle poden under rullerende utrulling.
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


-- Retter foreldet dokumentasjon fra V1.0_082: grunnlagsreferansen ble aldri brukt som nøkkel i etterlysning.
-- Etterlysningen nøkles på avklaringens referanse. Kolonnen skrives ved konstruksjon og leses aldri.
comment on column gr_bosatt_avklaring.grunnlag_ref is
    'Ubrukt. Etterlysning nøkles på bosatt_periode_avklaring(_foreslaatt).referanse, ikke på denne. Forsvinner med tabellen.';
