create table GR_BEREGNINSGRUNNLAG
(
    id                  bigint primary key,
    virkningsdato       date           not null,
    behandling_id       bigint         not null,
    aarsinntekt_bidrag_siste_aar     numeric(19, 2) not null,
    aarsinntekt_bidrag_tre_aar numeric(19, 2) not null,
    aarsinntekt_beste_beregning numeric(19, 2) not null,
    regel_input         oid            not null,
    regel_sporing       oid            not null,
    aktiv               boolean        not null default true,
    opprettet_av        character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid       timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av           character varying(20),
    endret_tid          timestamp(3) without time zone
);

alter table GR_BEREGNINSGRUNNLAG
    add constraint fk_gr_beregningsgrunnlag_behandling foreign key (behandling_id) references behandling (id);

CREATE SEQUENCE IF NOT EXISTS SEQ_GR_BEREGNINSGRUNNLAG START WITH 1000049 INCREMENT BY 50 MINVALUE 1000000 NO MAXVALUE CACHE 1;
