-- Migrering: forenkle bostedsgrunnlag og separat søknadsaggregat
--
-- Del 1: Erstatt bosatt_avklaring (sub-avklaringer) med direkte kolonner på bosatt_periode_avklaring
-- Del 2: Opprett nytt søknadsaggregat og backfill fra gammel søknadsholder
-- Del 3: Fjern soknad_avklaring_holder_id fra gr_bosatt_avklaring og drop bosatt_avklaring

-- Del 1a: Legg til nye kolonner på bosatt_periode_avklaring
alter table bosatt_periode_avklaring
    add column er_bosatt_i_trondheim boolean,
    add column fraflyttings_dato date;

-- Del 1b: Migrer data fra bosatt_avklaring
--   For hver periode: sett er_bosatt_i_trondheim = verdien til avklaringen med lavest fom_dato
--   Sett fraflyttings_dato = fom_dato til avklaringen med høyest fom_dato (dersom den ikke er bosatt)
update bosatt_periode_avklaring p
set er_bosatt_i_trondheim = (
    select a.er_bosatt_i_trondheim
    from bosatt_avklaring a
    where a.bosatt_periode_avklaring_id = p.id
    order by a.fom_dato
    limit 1
),
    fraflyttings_dato = (
        select case when not a.er_bosatt_i_trondheim then a.fom_dato else null end
        from bosatt_avklaring a
        where a.bosatt_periode_avklaring_id = p.id
        order by a.fom_dato desc
        limit 1
    )
where exists (
    select 1 from bosatt_avklaring a where a.bosatt_periode_avklaring_id = p.id
);

-- For perioder uten avklaringer (burde ikke forekomme, men sikrer NOT NULL):
update bosatt_periode_avklaring
set er_bosatt_i_trondheim = false
where er_bosatt_i_trondheim is null;

-- Del 1c: Sett er_bosatt_i_trondheim not null
alter table bosatt_periode_avklaring
    alter column er_bosatt_i_trondheim set not null;

-- Del 2a: Opprett seq for søknadsaggregat
create sequence seq_bosatt_soeknad_grunnlag increment by 50 minvalue 1000000;
create sequence seq_bosatt_informasjon_fra_soeknad increment by 50 minvalue 1000000;

-- Del 2b: Opprett bosatt_soeknad_grunnlag
create table bosatt_soeknad_grunnlag
(
    id            bigint                                 not null primary key,
    behandling_id bigint references behandling (id)     not null unique,
    opprettet_av  varchar(20)  default 'VL'              not null,
    opprettet_tid timestamp(3) default current_timestamp not null,
    endret_av     varchar(20),
    endret_tid    timestamp(3)
);

comment on table bosatt_soeknad_grunnlag is 'Søknadsbaserte bostedsopplysninger per behandling. Lagres additivt uten deaktivering.';

create index idx_bosatt_soeknad_grunnlag_behandling on bosatt_soeknad_grunnlag (behandling_id);

-- Del 2c: Opprett bosatt_informasjon_fra_soeknad
create table bosatt_informasjon_fra_soeknad
(
    id                          bigint                                             not null primary key,
    bosatt_soeknad_grunnlag_id  bigint references bosatt_soeknad_grunnlag (id)    not null,
    journalpost_id              varchar(50)                                        not null,
    fom_dato                    date                                               not null,
    er_bosatt_i_trondheim       boolean                                            not null,
    opprettet_av                varchar(20)  default 'VL'                          not null,
    opprettet_tid               timestamp(3) default current_timestamp             not null,
    endret_av                   varchar(20),
    endret_tid                  timestamp(3)
);

comment on table bosatt_informasjon_fra_soeknad is 'Bostedsopplysning oppgitt av bruker i søknaden. Koblet til bosatt_soeknad_grunnlag.';

create index idx_bosatt_informasjon_grunnlag on bosatt_informasjon_fra_soeknad (bosatt_soeknad_grunnlag_id);

-- Del 2d: Backfill søknadsdata fra gammel søknadsholder
--   Opprett bosatt_soeknad_grunnlag for behandlinger med aktiv søknadsholder
insert into bosatt_soeknad_grunnlag (id, behandling_id, opprettet_av, opprettet_tid)
select nextval('seq_bosatt_soeknad_grunnlag'), gr.behandling_id, 'MIGRASJON', current_timestamp
from gr_bosatt_avklaring gr
where gr.soknad_avklaring_holder_id is not null
  and gr.aktiv = true
on conflict do nothing;

--   Opprett bosatt_informasjon_fra_soeknad fra perioder i gammel søknadsholder
--   journalpost_id settes til 'MIGRERT' da denne ikke er tilgjengelig i gammelt skjema
--   fom_dato hentes fra skaeringstidspunkt på perioden, erBosattITrondheim fra første avklaring
insert into bosatt_informasjon_fra_soeknad (id, bosatt_soeknad_grunnlag_id, journalpost_id, fom_dato, er_bosatt_i_trondheim, opprettet_av, opprettet_tid)
select distinct on (sg.id, bpa.skaeringstidspunkt)
    nextval('seq_bosatt_informasjon_fra_soeknad'),
    sg.id,
    'MIGRERT',
    bpa.skaeringstidspunkt,
    ba.er_bosatt_i_trondheim,
    'MIGRASJON',
    current_timestamp
from gr_bosatt_avklaring gr
join bosatt_soeknad_grunnlag sg on sg.behandling_id = gr.behandling_id
join bosatt_periode_avklaring bpa on bpa.bosatt_avklaring_holder_id = gr.soknad_avklaring_holder_id
join bosatt_avklaring ba on ba.bosatt_periode_avklaring_id = bpa.id
where gr.aktiv = true
  and gr.soknad_avklaring_holder_id is not null
order by sg.id, bpa.skaeringstidspunkt, ba.fom_dato;

-- Del 3a: Drop bosatt_avklaring-tabellen (etter backfill)
drop table if exists bosatt_avklaring;

-- Del 3b: Drop seq_bosatt_avklaring
drop sequence if exists seq_bosatt_avklaring;

-- Del 3c: Fjern soknad_avklaring_holder_id fra gr_bosatt_avklaring
alter table gr_bosatt_avklaring
    drop column if exists soknad_avklaring_holder_id;

