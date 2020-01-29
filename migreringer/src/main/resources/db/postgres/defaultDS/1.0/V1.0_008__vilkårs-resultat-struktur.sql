ALTER TABLE vilkar_resultat
    RENAME TO VR_VILKAR_RESULTAT;
ALTER TABLE vilkar
    RENAME TO VR_VILKAR;
ALTER TABLE VILKAR_PERIODE
    RENAME TO VR_VILKAR_PERIODE;

create table RS_VILKARS_RESULTAT
(
    ID            bigint                              not null PRIMARY KEY,
    BEHANDLING_ID bigint                              not null,
    VILKARENE_ID  bigint,
    VERSJON       bigint       default 0              not null,
    AKTIV         boolean      default true           not null,
    OPPRETTET_AV  VARCHAR(20)  default 'VL'           not null,
    OPPRETTET_TID TIMESTAMP(3) default localtimestamp not null,
    ENDRET_AV     VARCHAR(20),
    ENDRET_TID    TIMESTAMP(3),
    constraint FK_RS_VILKARS_RESULTAT_1
        foreign key (BEHANDLING_ID) references behandling,
    constraint FK_RS_VILKARS_RESULTAT_2
        foreign key (VILKARENE_ID) references VR_VILKAR_RESULTAT
);

comment on table RS_VILKARS_RESULTAT is 'Behandlingsgrunnlag for arbeid, inntekt og ytelser (aggregat)';
comment on column RS_VILKARS_RESULTAT.ID is 'Primary Key';
comment on column RS_VILKARS_RESULTAT.BEHANDLING_ID is 'FK: BEHANDLING Fremmednøkkel for kobling til behandling';

create index IDX_RS_VILKARS_RESULTAT_1
    on RS_VILKARS_RESULTAT (BEHANDLING_ID);

create index IDX_RS_VILKARS_RESULTAT_2
    on RS_VILKARS_RESULTAT (VILKARENE_ID);

CREATE UNIQUE INDEX UIDX_RS_VILKARS_RESULTAT_01
    ON RS_VILKARS_RESULTAT (
                            (CASE
                                 WHEN AKTIV = true
                                     THEN BEHANDLING_ID
                                 ELSE NULL END),
                            (CASE
                                 WHEN AKTIV = true
                                     THEN AKTIV
                                 ELSE NULL END)
        );

ALTER TABLE VR_VILKAR_PERIODE ADD Column BEGRUNNELSE VARCHAR(4000);

ALTER TABLE opptjening add column behandling_id bigint references behandling;
ALTER TABLE opptjening drop vilkar_resultat_id cascade ;
ALTER TABLE BEHANDLING_RESULTAT drop inngangsvilkar_resultat_id cascade;


create sequence if not exists SEQ_RS_VILKARS_RESULTAT increment by 50 minvalue 1000000;
