create table RS_OPPTJENING
(
    ID            bigint                              not null PRIMARY KEY,
    BEHANDLING_ID bigint                              not null,
    VERSJON       bigint       default 0              not null,
    AKTIV         boolean      default true           not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    constraint FK_RS_OPPTJENING_01
        foreign key (BEHANDLING_ID) references behandling
);

create sequence if not exists SEQ_RS_OPPTJENING increment by 50 minvalue 1000000;

create index IDX_RS_OPPTJENING_01
    on RS_OPPTJENING (BEHANDLING_ID);

CREATE UNIQUE INDEX UIDX_RS_OPPTJENING_01 ON RS_OPPTJENING (BEHANDLING_ID) WHERE (AKTIV = TRUE);

ALTER TABLE opptjening
    add column opptjening_resultat_id bigint references RS_OPPTJENING;
create index IDX_opptjening_05
    on opptjening (opptjening_resultat_id);
