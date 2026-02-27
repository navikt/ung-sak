create table GR_BEREGNINGSGRUNNLAG
(
    id                              bigint primary key,
    behandling_id                   bigint         not null,
    virkningsdato                   date           not null,
    siste_lignede_aar               integer        not null,
    aarsinntekt_siste_aar           numeric(19, 2) not null,
    aarsinntekt_siste_tre_aar       numeric(19, 2) not null,
    aarsinntekt_beste_beregning     numeric(19, 2) not null,
    pgi1_aar                        integer        not null,
    pgi1_aarsinntekt                numeric(19, 2) not null,
    pgi2_aar                        integer        not null,
    pgi2_aarsinntekt                numeric(19, 2) not null,
    pgi3_aar                        integer        not null,
    pgi3_aarsinntekt                numeric(19, 2) not null,
    regel_sporing                   oid            not null,
    aktiv                           boolean        not null default true,
    opprettet_av        character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid       timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av           character varying(20),
    endret_tid          timestamp(3) without time zone
);

alter table GR_BEREGNINGSGRUNNLAG
    add constraint fk_gr_beregningsgrunnlag_behandling foreign key (behandling_id) references behandling (id);

CREATE SEQUENCE IF NOT EXISTS SEQ_GR_BEREGNINGSGRUNNLAG START WITH 1000049 INCREMENT BY 50 MINVALUE 1000000 NO MAXVALUE CACHE 1;
