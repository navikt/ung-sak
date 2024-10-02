create table if not exists AKSJONSPUNKT_SPORING
(
    ID                  bigint                              not null PRIMARY KEY,
    BEHANDLING_ID       bigint                              not null,
    AKSJONSPUNKT_DEF    VARCHAR(100)                        NOT NULL,
    PAYLOAD             JSON                                not null,
    OPPRETTET_TID       TIMESTAMP(3) default localtimestamp not null,
    constraint FK_AKSJONSPUNKT_SPORING_01
        foreign key (BEHANDLING_ID) references behandling
);

create sequence if not exists SEQ_AKSJONSPUNKT_SPORING increment by 50 minvalue 1000000;

comment on table AKSJONSPUNKT_SPORING is 'Lagrer data som ble brukt til bekreftelse av aksjonspunkt. Kun til bruk i diagnostikkformål. Data i tabellen utgjør ikke en del av behandlingsgrunnlaget.';
comment on column AKSJONSPUNKT_SPORING.BEHANDLING_ID is 'FK: Referanse til behandling';
comment on column AKSJONSPUNKT_SPORING.AKSJONSPUNKT_DEF is 'Aksjonspunktdefinisjon sporingen gjelder for.';
comment on column AKSJONSPUNKT_SPORING.PAYLOAD is 'Bekreftet opplysning i aksjonspunkt i json-format.';
comment on column AKSJONSPUNKT_SPORING.OPPRETTET_TID is 'Tidspunkt for opprettelse.';
