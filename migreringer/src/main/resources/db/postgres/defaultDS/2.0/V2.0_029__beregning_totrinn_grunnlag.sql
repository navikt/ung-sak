create table if not exists TT_BG_FAKTA_TILFELLE
(
    ID                       BIGINT                                 NOT NULL,
    FAKTA_BEREGNING_TILFELLE VARCHAR(100)                           NOT NULL,
    TT_BEREGNING_ID          BIGINT                                 NOT NULL,
    VERSJON                  BIGINT       DEFAULT 0                 NOT NULL,
    OPPRETTET_AV             VARCHAR(20)  DEFAULT 'VL'              NOT NULL,
    OPPRETTET_TID            TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ENDRET_AV                VARCHAR(20),
    ENDRET_TID               TIMESTAMP(3)
);
create sequence if not exists SEQ_TT_BG_FAKTA_TILFELLE increment by 50 minvalue 1000000;

create UNIQUE index PK_TT_BG_FAKTA_TILFELLE on TT_BG_FAKTA_TILFELLE(ID);
alter table TT_BG_FAKTA_TILFELLE
    add constraint PK_TT_BG_FAKTA_TILFELLE primary key using index PK_TT_BG_FAKTA_TILFELLE;
alter table TT_BG_FAKTA_TILFELLE
    add constraint FK_TT_BG_FAKTA_TILFELLE_1 foreign key (TT_BEREGNING_ID) references TT_BEREGNING(ID);
create index IDX_TT_BG_FAKTA_TILFELLE_1 on TT_BG_FAKTA_TILFELLE(TT_BEREGNING_ID);

alter table TT_BEREGNING add column FASTSATT_VARIG_ENDRING BOOLEAN;

comment on table TT_BG_FAKTA_TILFELLE is 'Koblingstabell mellom fakta om beregning tilfelle og totrinnsgrunnlag';
comment on column TT_BG_FAKTA_TILFELLE.ID is 'Primærnøkkel';
comment on column TT_BG_FAKTA_TILFELLE.FAKTA_BEREGNING_TILFELLE is 'Fakta om beregning tilfelle';
comment on column TT_BG_FAKTA_TILFELLE.TT_BEREGNING_ID is 'Fremmednøkkel til totrinnsvurdering';

comment on column TT_BEREGNING.FASTSATT_VARIG_ENDRING is 'Boolean som gir saksbehandlers vurdering av varig endring';
