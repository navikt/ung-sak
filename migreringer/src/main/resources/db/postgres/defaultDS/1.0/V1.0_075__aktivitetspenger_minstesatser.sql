create table AVP_GRUNNSATS_PERIODER
(
    id              bigint primary key,
    regel_input     oid not null,
    regel_sporing   oid not null,
    opprettet_av    character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid   timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av       character varying(20),
    endret_tid      timestamp(3) without time zone
);

CREATE SEQUENCE IF NOT EXISTS SEQ_AVP_GRUNNSATS_PERIODER START WITH 1000049 INCREMENT BY 50 MINVALUE 1000000 NO MAXVALUE CACHE 1;

create table AVP_GRUNNSATS_PERIODE
(
    id                        bigint primary key,
    avp_grunnsats_perioder_id bigint         not null,
    dagsats                   numeric(19, 2) not null,
    periode                   daterange       not null,
    grunnbeløp                numeric(19, 2) not null,
    grunnbeløp_faktor         numeric(19, 4) not null,
    sats_type                 character varying(50) not null,
    hjemmel                   character varying(50) not null,
    antall_barn               integer        not null,
    dagsats_barnetillegg      integer        not null,
    opprettet_av              character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid             timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av                 character varying(20),
    endret_tid                timestamp(3) without time zone
);

alter table AVP_GRUNNSATS_PERIODE
    add constraint fk_avp_grunnsats_periode_perioder foreign key (avp_grunnsats_perioder_id) references AVP_GRUNNSATS_PERIODER (id);

CREATE SEQUENCE IF NOT EXISTS SEQ_AVP_GRUNNSATS_PERIODE START WITH 1000049 INCREMENT BY 50 MINVALUE 1000000 NO MAXVALUE CACHE 1;

alter table AVP_GR_BEREGNINGSGRUNNLAG
    add column avp_grunnsats_perioder_id bigint;

alter table AVP_GR_BEREGNINGSGRUNNLAG
    add constraint fk_avp_gr_grunnsats_perioder foreign key (avp_grunnsats_perioder_id) references AVP_GRUNNSATS_PERIODER (id);


