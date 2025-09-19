alter table vedtaksbrev_valg
    add dokumentmal_type varchar(100) not null,
    add aktiv boolean not null default true;

drop index uidx_vedtaksbrev_valg_behandling_aktiv;

create index idx_vedtaksbrev_valg_behandling
    on vedtaksbrev_valg (behandling_id);

create unique index uidx_vedtaksbrev_valg_behandling_dokumentmaltype
    on vedtaksbrev_valg (behandling_id, dokumentmal_type)
    where aktiv = true;
