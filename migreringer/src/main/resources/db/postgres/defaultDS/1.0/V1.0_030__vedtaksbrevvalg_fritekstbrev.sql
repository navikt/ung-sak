create table if not exists vedtaksbrev_valg
(
    id                 bigint                                not null primary key,
    behandling_id      bigint                                not null references behandling (id),
    redigert           boolean                               not null,
    redigert_brev_html text,
    hindret            boolean                               not null,
    versjon            bigint      default 0                 not null,
    opprettet_av       varchar(20) default 'VL'              not null,
    opprettet_tid      timestamp   default CURRENT_TIMESTAMP not null,
    endret_av          varchar(20),
    endret_tid         timestamp,
    constraint fk_behandling foreign key (behandling_id) references behandling (id)
);

create unique index if not exists uidx_vedtaksbrev_valg_behandling_aktiv
    on vedtaksbrev_valg (behandling_id);

create sequence if not exists seq_vedtaksbrev_valg increment by 50 minvalue 1000000;
comment on column vedtaksbrev_valg.redigert is 'Indikerer om sakbehandler har redigert eller skrevet et fritekstbrev';
comment on column vedtaksbrev_valg.redigert_brev_html is 'Html tekst som skal brukes istedenfor det automatiske brevet ';
comment on column vedtaksbrev_valg.hindret is 'Indikerer om brevet er manuelt hindret';

comment on column vedtaksbrev_valg.versjon is 'Versjonsnummer av teksten for optimistisk låsing';
