create table uttalelse
(
    id              bigint      not null primary key,
    uttalelse      text        not null,
    etterlysning_id bigint      not null references etterlysning (id),
    opprettet_tid   timestamp            default CURRENT_TIMESTAMP not null,
    opprettet_av    varchar(20) not null default 'VL',
    endret_av       varchar(20),
    endret_tid      timestamp
);

create sequence if not exists seq_uttalelse increment by 50 minvalue 1000000;

comment on table uttalelse is 'Inneholder uttalelser typisk ved uenigheter på etterlysninger';
comment on column uttalelse.uttalelse is 'Uttalelsetekst';

alter table etterlysning add svar_journalpost_id varchar(20);
comment on column etterlysning.svar_journalpost_id is 'journalpost_id for svar på etterlysning';

create unique index idx_etterlysning_ekstern_ref on etterlysning (ekstern_ref);

