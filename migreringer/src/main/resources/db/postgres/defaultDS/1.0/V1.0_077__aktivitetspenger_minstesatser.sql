create table AVP_SATS_PERIODER
(
    id              bigint primary key,
    regel_input     oid not null,
    regel_sporing   oid not null,
    opprettet_av    character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid   timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av       character varying(20),
    endret_tid      timestamp(3) without time zone
);

CREATE SEQUENCE IF NOT EXISTS SEQ_AVP_SATS_PERIODER START WITH 1000049 INCREMENT BY 50 MINVALUE 1000000 NO MAXVALUE CACHE 1;

create table AVP_SATS_PERIODE
(
    id                       bigint primary key,
    avp_sats_perioder_id     bigint         not null,
    dagsats                  numeric(19, 2) not null,
    periode                  daterange       not null,
    grunnbeløp               numeric(19, 2) not null,
    grunnbeløp_faktor        numeric(19, 4) not null,
    sats_type                character varying(50) not null,
    hjemmel                  character varying(50) not null,
    antall_barn              integer        not null,
    dagsats_barnetillegg     integer        not null,
    minsteytelse             numeric(19, 2) not null,
    opprettet_av             character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid            timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av                character varying(20),
    endret_tid               timestamp(3) without time zone
);

COMMENT ON COLUMN AVP_SATS_PERIODE.avp_sats_perioder_id IS 'Referanse til samling av satsperioder';
COMMENT ON COLUMN AVP_SATS_PERIODE.dagsats IS 'Beregnet dagsats for perioden';
COMMENT ON COLUMN AVP_SATS_PERIODE.periode IS 'Perioden satsen gjelder for';
COMMENT ON COLUMN AVP_SATS_PERIODE.grunnbeløp IS 'Grunnbeløpet (G) benyttet i beregningen';
COMMENT ON COLUMN AVP_SATS_PERIODE.grunnbeløp_faktor IS 'Faktor for beregning av sats basert på grunnbeløp';
COMMENT ON COLUMN AVP_SATS_PERIODE.sats_type IS 'Satstype (høy/lav) som benyttes';
COMMENT ON COLUMN AVP_SATS_PERIODE.hjemmel IS 'Lovhjemmel for benyttet';
COMMENT ON COLUMN AVP_SATS_PERIODE.antall_barn IS 'Antall barn som gir rett til barnetillegg';
COMMENT ON COLUMN AVP_SATS_PERIODE.dagsats_barnetillegg IS 'Dagsats for barnetillegg';
COMMENT ON COLUMN AVP_SATS_PERIODE.minsteytelse IS 'Den garanterte årlige grunnytelsen som brukeren har rett til';

alter table AVP_SATS_PERIODE
    add constraint fk_avp_sats_periode_perioder foreign key (avp_sats_perioder_id) references AVP_SATS_PERIODER (id);

CREATE SEQUENCE IF NOT EXISTS SEQ_AVP_SATS_PERIODE START WITH 1000049 INCREMENT BY 50 MINVALUE 1000000 NO MAXVALUE CACHE 1;

ALTER TABLE AVP_GR_BEREGNINGSGRUNNLAG RENAME TO GR_AVP;
ALTER SEQUENCE SEQ_AVP_GR_BEREGNINGSGRUNNLAG RENAME TO SEQ_GR_AVP;
ALTER TABLE BEREGNINGSGRUNNLAG_KOBLING RENAME COLUMN avp_gr_beregningsgrunnlag_id TO gr_avp_id;

alter table GR_AVP
    add column avp_sats_perioder_id bigint;

alter table GR_AVP
    add constraint fk_avp_gr_sats_perioder foreign key (avp_sats_perioder_id) references AVP_SATS_PERIODER (id);
