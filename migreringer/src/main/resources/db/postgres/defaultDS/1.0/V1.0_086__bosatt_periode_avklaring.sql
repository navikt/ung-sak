-- Nytt aggregatlag mellom bosatt_avklaring_holder og bosatt_avklaring som holder
-- avklaringer per vilkårsperiode. Referansen brukes i etterlysning og uttalelse.

create sequence seq_bosatt_periode_avklaring increment by 50 start with 1000000;

create table bosatt_periode_avklaring
(
    id                          bigint primary key,
    bosatt_avklaring_holder_id  bigint      not null references bosatt_avklaring_holder (id),
    referanse                   uuid        not null unique,
    skaeringstidspunkt          date        not null,
    versjon                     bigint      not null default 0,
    opprettet_av                varchar(20) not null default 'VL',
    opprettet_tid               timestamp(3) not null default current_timestamp,
    endret_av                   varchar(20),
    endret_tid                  timestamp(3)
);

create index idx_bosatt_periode_avklaring_holder on bosatt_periode_avklaring (bosatt_avklaring_holder_id);

-- Migrer eksisterende data: én periodeAvklaring per bosatt_avklaring, med skaeringstidspunkt=fom_dato
insert into bosatt_periode_avklaring (id, bosatt_avklaring_holder_id, referanse, skaeringstidspunkt)
select nextval('seq_bosatt_periode_avklaring'),
       ba.bosatt_avklaring_holder_id,
       gen_random_uuid(),
       ba.fom_dato
from bosatt_avklaring ba;

-- Legg til ny FK-kolonne i bosatt_avklaring som peker på det nye laget
alter table bosatt_avklaring
    add column bosatt_periode_avklaring_id bigint references bosatt_periode_avklaring (id);

-- Fyll inn referansen basert på matchende holder_id og fom_dato
update bosatt_avklaring ba
set bosatt_periode_avklaring_id = (
    select pa.id
    from bosatt_periode_avklaring pa
    where pa.bosatt_avklaring_holder_id = ba.bosatt_avklaring_holder_id
      and pa.skaeringstidspunkt = ba.fom_dato
);

alter table bosatt_avklaring
    alter column bosatt_periode_avklaring_id set not null;

-- Fjern gammel direktekobling fra avklaring til holder
alter table bosatt_avklaring
    drop column bosatt_avklaring_holder_id;
