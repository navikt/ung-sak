CREATE TABLE IF NOT EXISTS behandling_ansvarlig (
    id bigint primary key,
    behandling_id bigint NOT NULL,
    behandling_del character varying (30) NOT NULL,
    versjon bigint DEFAULT 0 NOT NULL,

    ansvarlig_saksbehandler character varying(100),
    ansvarlig_beslutter character varying(100),
    behandlende_enhet character varying(10),
    behandlende_enhet_navn character varying(320),
    behandlende_enhet_arsak character varying(800),

    totrinnsbehandling boolean DEFAULT false NOT NULL,

    opprettet_av character varying(20) DEFAULT 'VL'::character varying NOT NULL,
    opprettet_tid timestamp(3) without time zone DEFAULT timezone('utc'::text, now()) NOT NULL,
    endret_av character varying(20),
    endret_tid timestamp(3) without time zone
);

alter table behandling_ansvarlig add constraint fk_behandling_ansvarlig_behandling_id foreign key (behandling_id) references behandling (id);
create unique index uidx_behandling_ansvarlig_1 on behandling_ansvarlig (behandling_id, behandling_del);

comment on table behandling_ansvarlig is 'Holder oversikt over hvem som er ansvarlig for en behandling, og hvilken enhet som behandler den.';
COMMENT ON COLUMN behandling_ansvarlig.behandling_id IS 'Peker på aktuell behandling';
COMMENT ON COLUMN behandling_ansvarlig.behandling_del IS 'Indikerer om ansvaret gjelder hele behandlingen, eller deler av behandlingen.';
COMMENT ON COLUMN behandling_ansvarlig.ansvarlig_saksbehandler IS 'Id til saksbehandler som oppretter forslag til vedtak ved totrinnsbehandling.';
COMMENT ON COLUMN behandling_ansvarlig.ansvarlig_beslutter IS 'Beslutter som har fattet vedtaket';
COMMENT ON COLUMN behandling_ansvarlig.behandlende_enhet IS 'NAV-enhet som behandler behandlingen';
COMMENT ON COLUMN behandling_ansvarlig.behandlende_enhet_navn IS 'Navn på behandlende enhet';
COMMENT ON COLUMN behandling_ansvarlig.behandlende_enhet_arsak IS 'Fritekst for hvorfor behandlende enhet har blitt endret';
COMMENT ON COLUMN behandling_ansvarlig.totrinnsbehandling IS 'Indikerer at behandlingen skal totrinnsbehandles';

CREATE SEQUENCE IF NOT EXISTS seq_behandling_ansvarlig START WITH 3000049 INCREMENT BY 50 MINVALUE 3000000;
