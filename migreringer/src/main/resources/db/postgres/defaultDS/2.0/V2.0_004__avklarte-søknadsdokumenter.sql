create table if not exists SF_AVKLART_DOKUMENTER
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    VERSJON       BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table if not exists SF_AVKLART_DOKUMENT
(
    ID             BIGINT                                 NOT NULL PRIMARY KEY,
    dokumenter_ID  BIGINT REFERENCES SF_AVKLART_DOKUMENTER (id),
    JOURNALPOST_ID VARCHAR(20)                            NOT NULL,
    godkjent       boolean                                NOT NULL,
    gyldig_fra     date,
    BEGRUNNELSE    VARCHAR(4000)                          NOT NULL,
    VERSJON        BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV   VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID  TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV      VARCHAR(20),
    ENDRET_TID     TIMESTAMP(3)
);
create index IDX_SF_AVKLART_DOKUMENT_1
    on SF_AVKLART_DOKUMENT (dokumenter_ID);

create table RS_SOKNADSFRIST
(
    ID            bigint                              not null PRIMARY KEY,
    BEHANDLING_ID bigint                              not null,
    overstyrt_id  bigint,
    avklart_id    bigint,
    VERSJON       bigint       default 0              not null,
    AKTIV         boolean      default true           not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    constraint FK_RS_SOKNADSFRIST_1
        foreign key (BEHANDLING_ID) references BEHANDLING,
    constraint FK_RS_SOKNADSFRIST_2
        foreign key (overstyrt_id) references SF_AVKLART_DOKUMENTER,
    constraint FK_RS_SOKNADSFRIST_3
        foreign key (avklart_id) references SF_AVKLART_DOKUMENTER
);

create sequence if not exists SEQ_SF_AVKLART_DOKUMENT increment by 50 minvalue 1000000;
create sequence if not exists SEQ_SF_AVKLART_DOKUMENTER increment by 50 minvalue 1000000;
create sequence if not exists SEQ_RS_SOKNADSFRIST increment by 50 minvalue 1000000;

create index IDX_RS_SOKNADSFRIST_1
    on RS_SOKNADSFRIST (BEHANDLING_ID);

create index IDX_RS_SOKNADSFRIST_2
    on RS_SOKNADSFRIST (overstyrt_id);
create index IDX_RS_SOKNADSFRIST_3
    on RS_SOKNADSFRIST (avklart_id);

CREATE UNIQUE INDEX UIDX_RS_SOKNADSFRIST_01 ON RS_SOKNADSFRIST (BEHANDLING_ID) WHERE (AKTIV = TRUE);
