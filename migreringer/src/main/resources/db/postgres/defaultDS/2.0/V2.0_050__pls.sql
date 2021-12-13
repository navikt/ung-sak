create table DOKUMENT
(
    ID             BIGINT                                 NOT NULL PRIMARY KEY,
    JOURNALPOST_ID VARCHAR(20)                            not null,
    SYK_PERSON_ID  BIGINT                                 NOT NULL,
    OPPRETTET_AV   VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID  TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    CONSTRAINT FK_SYKDOM_VURDERINGER_01
        FOREIGN KEY (SYK_PERSON_ID) REFERENCES SYKDOM_PERSON (ID),
    UNIQUE (JOURNALPOST_ID)
);


create table VURDERING
(
    ID             BIGINT                                 NOT NULL PRIMARY KEY,
    JOURNALPOST_ID VARCHAR(20)                            not null,
    RESULTAT       VARCHAR(20)                            NOT NULL,
    OPPRETTET_AV   VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID  TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);

create table GR_PLEIEBEHOV_GRUNNLAG
(
    ID            bigint                              not null PRIMARY KEY,
    BEHANDLING_ID bigint REFERENCES BEHANDLING (id)   not null,
    VURDERING_ID  bigint REFERENCES VURDERING (id)    not null,
    VERSJON       bigint       default 0              not null,
    AKTIV         boolean      default true           not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3)
);
