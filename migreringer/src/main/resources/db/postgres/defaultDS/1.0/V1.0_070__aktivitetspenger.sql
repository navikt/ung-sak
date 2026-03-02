create table akt_soekt_periode
(
    id                      bigint primary key,
    behandling_id           bigint       not null,
    fom                     date         not null,
    tom                     date         not null,
    journalpost_id          varchar(30)  not null,
    journalpost_mottatt_tid timestamp(3) not null,

    versjon                 bigint       not null default 0,
    opprettet_av            character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid           timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av               character varying(20),
    endret_tid              timestamp(3) without time zone
);

create unique index uidx_akt_soekt_periode_1 on akt_soekt_periode (behandling_id, journalpost_id);
alter table akt_soekt_periode
    add constraint fk_aktivitetspenger_behandling foreign key (behandling_id) references behandling (id);

CREATE SEQUENCE IF NOT EXISTS seq_akt_soekt_periode START WITH 1000049 INCREMENT BY 50 MINVALUE 1000000 NO MAXVALUE CACHE 1
