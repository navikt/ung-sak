create table BEREGNINGSGRUNNLAG
(
    id                              bigint primary key,
    skjaeringstidspunkt             date           not null,
    siste_lignede_aar               integer        not null,
    aarsinntekt_siste_aar           numeric(19, 2) not null,
    aarsinntekt_siste_tre_aar       numeric(19, 2) not null,
    beregnet_pr_aar                 numeric(19, 2) not null,
    beregnet_redusert_pr_aar        numeric(19, 2) not null,
    pgi1_aar                        integer        not null,
    pgi1_aarsinntekt                numeric(19, 2) not null,
    pgi2_aar                        integer        not null,
    pgi2_aarsinntekt                numeric(19, 2) not null,
    pgi3_aar                        integer        not null,
    pgi3_aarsinntekt                numeric(19, 2) not null,
    regel_sporing                   oid            not null,
    opprettet_av        character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid       timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av           character varying(20),
    endret_tid          timestamp(3) without time zone
);

CREATE SEQUENCE IF NOT EXISTS SEQ_BEREGNINGSGRUNNLAG START WITH 1000049 INCREMENT BY 50 MINVALUE 1000000 NO MAXVALUE CACHE 1;

COMMENT ON COLUMN BEREGNINGSGRUNNLAG.skjaeringstidspunkt          IS 'Input til beregningen: skjæringstidspunkt';
COMMENT ON COLUMN BEREGNINGSGRUNNLAG.siste_lignede_aar            IS 'Input til beregningen: seneste tilgjengelige ligningsår (tilsvarer pgi3_aar)';
COMMENT ON COLUMN BEREGNINGSGRUNNLAG.aarsinntekt_siste_aar        IS 'Samlet inntekt (avkortet og oppjustert) for seneste ligningsår';
COMMENT ON COLUMN BEREGNINGSGRUNNLAG.aarsinntekt_siste_tre_aar    IS 'Samlet inntekt (avkortet og oppjustert) snitt over siste tre ligningsår';
COMMENT ON COLUMN BEREGNINGSGRUNNLAG.beregnet_pr_aar              IS 'Beregningsgrunnlag per år (maks av siste år og snitt siste tre år)';
COMMENT ON COLUMN BEREGNINGSGRUNNLAG.beregnet_redusert_pr_aar     IS 'Beregningsgrunnlag per år etter reduksjon med dekningsgrad';
COMMENT ON COLUMN BEREGNINGSGRUNNLAG.pgi1_aar                     IS 'Input til beregningen: år for PGI tidligste år (siste_lignede_aar - 2)';
COMMENT ON COLUMN BEREGNINGSGRUNNLAG.pgi1_aarsinntekt             IS 'Input til beregningen: rå PGI for tidligste år';
COMMENT ON COLUMN BEREGNINGSGRUNNLAG.pgi2_aar                     IS 'Input til beregningen: år for PGI midterste år (siste_lignede_aar - 1)';
COMMENT ON COLUMN BEREGNINGSGRUNNLAG.pgi2_aarsinntekt             IS 'Input til beregningen: rå PGI for midterste år';
COMMENT ON COLUMN BEREGNINGSGRUNNLAG.pgi3_aar                     IS 'Input til beregningen: år for PGI seneste år (siste_lignede_aar)';
COMMENT ON COLUMN BEREGNINGSGRUNNLAG.pgi3_aarsinntekt             IS 'Input til beregningen: rå PGI for seneste år';
COMMENT ON COLUMN BEREGNINGSGRUNNLAG.regel_sporing                IS 'Sporingsdata for regelkjøring';


create table AVP_GR_BEREGNINGSGRUNNLAG
(
    id                        bigint primary key,
    behandling_id             bigint not null,
    aktiv                     boolean not null default true,
    versjon                   bigint not null default 0,
    opprettet_av              character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid             timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av                 character varying(20),
    endret_tid                timestamp(3) without time zone
);

alter table AVP_GR_BEREGNINGSGRUNNLAG
    add constraint fk_avp_gr_behandling foreign key (behandling_id) references behandling (id);

CREATE UNIQUE INDEX UIDX_AVP_GR_BEREGNINGSGRUNNLAG ON AVP_GR_BEREGNINGSGRUNNLAG (behandling_id) WHERE aktiv = true;

CREATE SEQUENCE IF NOT EXISTS SEQ_AVP_GR_BEREGNINGSGRUNNLAG START WITH 1000049 INCREMENT BY 50 MINVALUE 1000000 NO MAXVALUE CACHE 1;

COMMENT ON COLUMN AVP_GR_BEREGNINGSGRUNNLAG.behandling_id IS 'Behandlingen dette beregningsgrunnlaget tilhører';
COMMENT ON COLUMN AVP_GR_BEREGNINGSGRUNNLAG.aktiv         IS 'Kun ett grunnlag er aktivt per behandling. Ved endring deaktiveres eksisterende og nytt grunnlag opprettes';

CREATE TABLE BEREGNINGSGRUNNLAG_KOBLING
(
    avp_gr_beregningsgrunnlag_id BIGINT NOT NULL,
    beregningsgrunnlag_id        BIGINT NOT NULL,
    CONSTRAINT pk_bg_kobling PRIMARY KEY (avp_gr_beregningsgrunnlag_id, beregningsgrunnlag_id),
    CONSTRAINT fk_bg_kobling_gr FOREIGN KEY (avp_gr_beregningsgrunnlag_id) REFERENCES AVP_GR_BEREGNINGSGRUNNLAG (id),
    CONSTRAINT fk_bg_kobling_bg FOREIGN KEY (beregningsgrunnlag_id) REFERENCES BEREGNINGSGRUNNLAG (id)
);

