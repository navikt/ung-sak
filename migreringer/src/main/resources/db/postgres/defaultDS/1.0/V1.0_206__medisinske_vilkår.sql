create table if not exists SYKDOM_PERSON
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    AKTOER_ID     VARCHAR(50)                            NOT NULL UNIQUE,
    NORSK_IDENTITETSNUMMER  VARCHAR(50)                  NOT NULL UNIQUE
);
create sequence if not exists SEQ_SYKDOM_PERSON increment by 5 minvalue 1000000;

create table if not exists SYKDOM_VURDERINGER
(
    ID              BIGINT                               NOT NULL PRIMARY KEY,
    SYK_PERSON_ID   BIGINT                               NOT NULL,

    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT FK_SYKDOM_VURDERINGER_01
        FOREIGN_KEY(SYK_PERSON_ID) REFERENCES SYKDOM_PERSON(ID)
);
create sequence if not exists SEQ_SYKDOM_VURDERINGER increment by 5 minvalue 1000000;

create table if not exists SYKDOM_VURDERING
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    TYPE          VARCHAR(20)                           NOT NULL,
    SYKDOM_VURDERINGER_ID   BIGINT                      NOT NULL,
    RANGERING               BIGINT                      NOT NULL,

    VERSJON       BIGINT       DEFAULT 0                NOT NULL,
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT FK_SYKDOM_VURDERING_01
        FOREIGN KEY(SYKDOM_VURDERINGER_ID) REFERENCES SYKDOM_VURDERINGER(ID),
    UNIQUE(
        SYKDOM_VURDERINGER_ID,
        SYKDOM_VURDERING_RANGERING,
        TYPE)
);
create sequence if not exists SEQ_SYKDOM_VURDERING increment by 5 minvalue 1000000;

create table if not exists SYKDOM_VURDERING_VERSJON
(
    ID                      BIGINT                      NOT NULL PRIMARY KEY,
    SYKDOM_VURDERING_ID     BIGINT                      NOT NULL,
    TEKST         VARCHAR(4000)                         NOT NULL,
    RESULTAT      VARCHAR(20)                           NOT NULL,

    VERSJON       BIGINT       DEFAULT 0                NOT NULL,
    ENDRET_AV     VARCHAR(20)                           NOT NULL,
    ENDRET_TID              TIMESTAMP(3)                NOT NULL,
    ENDRET_BEHANDLING_UUID   UUID                       NOT NULL,
    ENDRET_SAKSNUMMER       VARCHAR(19)                 NOT NULL,
    ENDRET_FOR_PERSON_ID    BIGINT                      NOT NULL,
    CONSTRAINT FK_SYKDOM_VURDERING_VERSJON_01
        FOREIGN KEY(SYKDOM_VURDERING_ID) REFERENCES SYKDOM_VURDERING(ID),
    CONSTRAINT FK_SYKDOM_VURDERING_VERSJON_02
        FOREIGN KEY(ENDRET_FOR_PERSON_ID) REFERENCES PERSON(ID)
    UNIQUE(SYKDOM_VURDERING_ID, VERSJON)
);
create sequence if not exists SEQ_SYKDOM_VURDERING_VERSJON increment by 5 minvalue 1000000;

create table if not exists SYKDOM_VURDERING_VERSJON_BESLUTTET
(
    "SYKDOM_VURDERING_VERSJON_ID"     BIGINT                  NOT NULL PRIMARY KEY,

    ENDRET_AV               VARCHAR(20)                 NOT NULL,
    ENDRET_TID              TIMESTAMP(3)                NOT NULL,
    CONSTRAINT FK_SYKDOM_VURDERING_VERSJON_BESLUTTET_01
        FOREIGN KEY(SYKDOM_VURDERING_VERSJON_ID) REFERENCES SYKDOM_VURDERING_VERSJON(ID)
);
create sequence if not exists SEQ_SYKDOM_VURDERING_VERSJON_BESLUTTET increment by 5 minvalue 1000000;

create table if not exists SYKDOM_PERIODE
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    SYKDOM_VURDERING_VERSJON_ID BIGINT                  NOT NULL,
    FOM             DATE                                NOT NULL,
    TOM             DATE                                NOT NULL,

    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT FK_SYKDOM_PERIODE_01
        FOREIGN KEY(SYKDOM_VURDERING_VERSJON_ID) REFERENCES SYKDOM_VURDERING_VERSJON(ID)
);
create sequence if not exists SEQ_SYKDOM_PERIODE increment by 5 minvalue 1000000;

create table if not exists SYKDOM_DOKUMENT
(
    ID                  BIGINT                             NOT NULL PRIMARY KEY,
    JOURNALPOST_ID      VARCHAR(50)                        NOT NULL,
    DOKUMENT_INFO_ID    VARCHAR(50)                        NOT NULL,
    --variant?
    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'                NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    ENDRET_AV     VARCHAR(20)                              NOT NULL,
    ENDRET_TID    TIMESTAMP(3)                             NOT NULL,
    UNIQUE(JOURNALPOST_ID, DOKUMENT_INFO_ID)
);
create sequence if not exists SEQ_SYKDOM_DOKUMENT increment by 5 minvalue 1000000;

create table if not exists SYKDOM_DOKUMENT_SAK
(
    ID                  BIGINT                             NOT NULL PRIMARY KEY,
    SYKDOM_DOKUMENT_ID  BIGINT                             NOT NULL,
    SAK_PERSON_ID       BIGINT                             NOT NULL,
    SAKSNUMMMER         VARCHAR(19)                        NOT NULL,

    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'                NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP   NOT NULL,
    CONSTRAINT FK_SYKDOM_DOKUMENT_SAK_01
        FOREIGN KEY SYKDOM_DOKUMENT_ID REFERENCES SYKDOM_DOKUMENT(ID)
);
create sequence if not exists SEQ_SYKDOM_DOKUMENT_SAK increment by 5 minvalue 1000000;

create table if not exists SYKDOM_VURDERING_VERSJON_DOKUMENT
(
    ID                          BIGINT                  NOT NULL PRIMARY KEY,
    SYKDOM_DOKUMENT_ID          BIGINT                  NOT NULL,
    SYKDOM_VURDERING_VERSJON_ID BIGINT                  NOT NULL,

    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT FK_SYKDOM_VURDERING_VERSJON_DOKUMENT_01
        FOREIGN KEY(SYKDOM_DOKUMENT_ID) REFERENCES SYKDOM_DOKUMENT(ID),
    CONSTRAINT FK_SYKDOM_VURDERING_VERSJON_DOKUMENT_02
        FOREIGN KEY(SYKDOM_VURDERING_VERSJON_ID) REFERENCES SYKDOM_VURDERING_VERSJON(ID)
);
create sequence if not exists SEQ_SYKDOM_VURDERING_VERSJON_DOKUMENT increment by 5 minvalue 1000000;

------------------------- SØKERS FAGSAK HERFRA

create table if not exists SYKDOM_GRUNNLAG
(
    ID                      BIGINT                      NOT NULL PRIMARY KEY,
    SYKDOM_GRUNNLAG_UUID    UUID                        NOT NULL,

    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL
);
create sequence if not exists SEQ_SYKDOM_GRUNNLAG increment by 5 minvalue 1000000;

create table if not exists SYKDOM_GRUNNLAG_BEHANDLING
(
    ID                      BIGINT                      NOT NULL PRIMARY KEY,
    SYKDOM_GRUNNLAG_ID      BIGINT                      NOT NULL,
    SOEKER_PERSON_ID        BIGINT                      NOT NULL,
    SAKSNUMMMER             VARCHAR(19)                 NOT NULL,
    BEHANDLING_UUID         UUID                        NOT NULL,
    BEHANDLINGSNUMMER       BIGINT                      NOT NULL,
    VERSJON                 BIGINT  DEFAULT 0           NOT NULL,

    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT FK_SYKDOM_GRUNNLAG_BEHANDLING_01
        FOREIGN KEY(SYKDOM_GRUNNLAG_ID) REFERENCES SYKDOM_GRUNNLAG(ID),
    CONSTRAINT FK_SYKDOM_GRUNNLAG_BEHANDLING_01
        FOREIGN KEY(PERSON_ID) REFERENCES PERSON(ID),
    UNIQUE(BEHANDLING_UUID, VERSJON),
    UNIQUE(SAKSNUMMER, BEHANDLINGSNUMMER, VERSJON)
);
create sequence if not exists SEQ_SYKDOM_GRUNNLAG_BEHANDLING increment by 5 minvalue 1000000;

create table if not exists SYKDOM_GRUNNLAG_VURDERING
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    SYKDOM_GRUNNLAG_ID          BIGINT                      NOT NULL,
    SYKDOM_VURDERING_VERSJON_ID BIGINT                      NOT NULL,

    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT FK_SYKDOM_GRUNNLAG_VURDERING_01
        FOREIGN KEY(SYKDOM_GRUNNLAG_ID) REFERENCES SYKDOM_GRUNNLAG(ID),
    CONSTRAINT FK_SYKDOM_GRUNNLAG_VURDERING_02
        FOREIGN KEY(SYKDOM_VURDERING_VERSJON_ID) REFERENCES SYKDOM_VURDERING_VERSJON(ID)
);
create sequence if not exists SEQ_SYKDOM_GRUNNLAG_VURDERING increment by 5 minvalue 1000000;

create table if not exists SYKDOM_SOEKT_PERIODE
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    SYKDOM_GRUNNLAG_ID          BIGINT                      NOT NULL,
    FOM             DATE                                NOT NULL,
    TOM             DATE                                NOT NULL,

    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINt FK_SYKDOM_SOEKT_PERIODE_01
        FOREIGN KEY(SYKDOM_GRUNNLAG_ID) REFERENCES SYKDOM_GRUNNLAG(ID)
);
create sequence if not exists SEQ_SYKDOM_SOEKT_PERIODE increment by 5 minvalue 1000000;

create table if not exists SYKDOM_REVURDERING_PERIODE
(
    ID            BIGINT                                 NOT NULL PRIMARY KEY,
    SYKDOM_GRUNNLAG_ID          BIGINT                      NOT NULL,
    FOM             DATE                                NOT NULL,
    TOM             DATE                                NOT NULL,

    OPPRETTET_AV  VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINt FK_SYKDOM_REVURDERING_PERIODE_01
        FOREIGN KEY(SYKDOM_GRUNNLAG_ID) REFERENCES SYKDOM_GRUNNLAG(ID)
);
create sequence if not exists SEQ_SYKDOM_REVURDERING_PERIODE increment by 5 minvalue 1000000;
