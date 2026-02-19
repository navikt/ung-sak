create table akt_beste_beregning_gr
(
    id                  bigint primary key,
    behandling_id       bigint         not null,
    aarsinntekt_siste_aar     numeric(19, 2) not null,
    aarsinntekt_siste_tre_aar numeric(19, 2) not null,
    aarsinntekt_beste_beregning numeric(19, 2) not null,
    regel_input         text           not null,
    regel_sporing       text           not null,
    aktiv               boolean        not null default true,
    versjon             bigint         not null default 0,
    opprettet_av        character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid       timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av           character varying(20),
    endret_tid          timestamp(3) without time zone
);

alter table akt_beste_beregning_gr
    add constraint fk_akt_beste_beregning_gr_behandling foreign key (behandling_id) references behandling (id);

CREATE SEQUENCE IF NOT EXISTS SEQ_AKT_BESTE_BEREGNING_GR START WITH 1000049 INCREMENT BY 50 MINVALUE 1000000 NO MAXVALUE CACHE 1;
