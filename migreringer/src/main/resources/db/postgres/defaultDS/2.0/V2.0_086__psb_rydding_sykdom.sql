alter table SYKDOM_PERSON rename to PERSON;

alter table SYKDOM_VURDERINGER rename to PLEIETRENGENDE_SYKDOM;
alter table PLEIETRENGENDE_SYKDOM rename column syk_person_id to pleietrengende_person_id;
alter table PLEIETRENGENDE_SYKDOM rename constraint "sykdom_vurderinger_pkey" to "pleietrengende_sykdom_pkey";
alter table PLEIETRENGENDE_SYKDOM rename constraint "sykdom_vurderinger_syk_person_id_key" to "pleietrengende_sykdom_pleietrengende_person_id_key";
alter table PLEIETRENGENDE_SYKDOM rename constraint "fk_sykdom_vurderinger_01" to "fk_pleietrengende_sykdom_01";

-- diagnoser
alter table SYKDOM_DIAGNOSEKODER rename to PLEIETRENGENDE_SYKDOM_DIAGNOSER;
alter table PLEIETRENGENDE_SYKDOM_DIAGNOSER rename column sykdom_vurderinger_id to pleietrengende_sykdom_id;
alter table PLEIETRENGENDE_SYKDOM_DIAGNOSER rename constraint "sykdom_diagnosekoder_pkey" to "pleietrengende_sykdom_diagnoser_pkey";
alter table PLEIETRENGENDE_SYKDOM_DIAGNOSER rename constraint "sykdom_diagnosekoder_sykdom_vurderinger_id_versjon_key" to "pleietrengende_syk_diag_pleietrengende_sykdom_id_versjon_key";
alter table PLEIETRENGENDE_SYKDOM_DIAGNOSER rename constraint "fk_sykdom_diagnosekoder_01" to "fk_pleietrengende_sykdom_diagnoser_01";

alter table SYKDOM_DIAGNOSEKODE rename to PLEIETRENGENDE_SYKDOM_DIAGNOSE;
alter table PLEIETRENGENDE_SYKDOM_DIAGNOSE rename column sykdom_diagnosekoder_id to pleietrengende_sykdom_diagnoser_id;
alter table PLEIETRENGENDE_SYKDOM_DIAGNOSE rename constraint "sykdom_diagnosekode_pkey" to "pleietrengende_sykdom_diagnose_pkey";
alter table PLEIETRENGENDE_SYKDOM_DIAGNOSE rename constraint "sykdom_diagnosekode_sykdom_diagnosekoder_id_diagnosekode_key" to "pleietrengende_sykdom_diagnose_diagnoser_id_diagnosekode_key";
alter table PLEIETRENGENDE_SYKDOM_DIAGNOSE rename constraint "fk_sykdom_diagnosekode_01" to "fk_pleietrengende_sykdom_diagnose_01";

-- vurderinger
alter table SYKDOM_VURDERING rename to PLEIETRENGENDE_SYKDOM_VURDERING;
alter table PLEIETRENGENDE_SYKDOM_VURDERING rename column sykdom_vurderinger_id to pleietrengende_sykdom_id;
alter table PLEIETRENGENDE_SYKDOM_VURDERING rename constraint sykdom_vurdering_pkey to pleietrengende_sykdom_vurdering_pkey;
alter table PLEIETRENGENDE_SYKDOM_VURDERING rename constraint sykdom_vurdering_sykdom_vurderinger_id_rangering_type_key to pleietrengende_sykdom_vurdering_vurdid_rangering_type_key;
alter table PLEIETRENGENDE_SYKDOM_VURDERING rename constraint fk_sykdom_vurdering_01 to fk_pleietrengende_sykdom_vurdering_01;

alter table SYKDOM_VURDERING_VERSJON rename to PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON rename column sykdom_vurdering_id to pleietrengende_sykdom_vurdering_id;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON rename column endret_for_person_id to endret_for_soeker;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON rename column endret_saksnummer to endret_for_soekers_saksnummer;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON rename column endret_behandling_uuid to endret_for_soekers_behandling_uuid;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON rename constraint sykdom_vurdering_versjon_pkey to pleietrengende_sykdom_vurdering_versjon_pkey;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON rename constraint sykdom_vurdering_versjon_sykdom_vurdering_id_versjon_key to pleietrengende_sykdom_vurdering_versjon_vurdid_versjon_key;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON rename constraint fk_sykdom_vurdering_versjon_01 to fk_pleietrengende_sykdom_vurdering_versjon_01;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON rename constraint fk_sykdom_vurdering_versjon_02 to fk_pleietrengende_sykdom_vurdering_versjon_02;

alter table SYKDOM_VURDERING_PERIODE rename to PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_PERIODE;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_PERIODE rename column sykdom_vurdering_versjon_id to pleietrengende_sykdom_vurdering_versjon_id;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_PERIODE rename constraint sykdom_vurdering_periode_pkey to pleietrengende_sykdom_vurdering_versjon_periode_pkey;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_PERIODE rename constraint fk_sykdom_vurdering_periode_01 to fk_pleietrengende_sykdom_vurdering_versjon_periode_01;

alter table SYKDOM_VURDERING_VERSJON_BESLUTTET rename to PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_BESLUTTET;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_BESLUTTET rename column sykdom_vurdering_versjon_id to pleietrengende_sykdom_vurdering_versjon_id;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_BESLUTTET rename constraint sykdom_vurdering_versjon_besluttet_pkey to pleietrengende_sykdom_vurdering_versjon_besluttet_pkey;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_BESLUTTET rename constraint fk_sykdom_vurdering_versjon_besluttet_01 to fk_pleietrengende_sykdom_vurdering_versjon_besluttet_01;

-- innleggelser
alter table SYKDOM_INNLEGGELSER rename to PLEIETRENGENDE_SYKDOM_INNLEGGELSER;
alter table PLEIETRENGENDE_SYKDOM_INNLEGGELSER rename column sykdom_vurderinger_id to pleietrengende_sykdom_id;
alter table PLEIETRENGENDE_SYKDOM_INNLEGGELSER rename constraint sykdom_innleggelser_pkey to pleietrengende_sykdom_innleggelser_pkey;
alter table PLEIETRENGENDE_SYKDOM_INNLEGGELSER rename constraint sykdom_innleggelser_sykdom_vurderinger_id_versjon_key to pleietrengende_sykdom_innleggelser_sykdom_id_versjon_key;

alter table SYKDOM_INNLEGGELSE_PERIODE rename to PLEIETRENGENDE_SYKDOM_INNLEGGELSE_PERIODE;
alter table PLEIETRENGENDE_SYKDOM_INNLEGGELSE_PERIODE rename column sykdom_innleggelser_id to pleietrengende_sykdom_innleggelser_id;
alter table PLEIETRENGENDE_SYKDOM_INNLEGGELSE_PERIODE rename constraint sykdom_innleggelse_periode_pkey to pleietrengende_sykdom_innleggelse_periode_pkey;
alter table PLEIETRENGENDE_SYKDOM_INNLEGGELSE_PERIODE rename constraint fk_sykdom_innleggelse_periode_01 to fk_pleietrengende_sykdom_innleggelse_periode_01;


-- dokumenter
alter table SYKDOM_DOKUMENT rename to PLEIETRENGENDE_SYKDOM_DOKUMENT;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT rename column sykdom_vurderinger_id to pleietrengende_sykdom_id;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT rename column behandling_uuid to soekers_behandling_uuid;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT rename column saksnummer to soekers_saksnummer;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT rename column person_id to soekers_person_id;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT rename constraint sykdom_dokument_pkey to pleietrengende_sykdom_dokument_pkey;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT rename constraint sykdom_dokument_journalpost_id_dokument_info_id_key to pleietrengende_sykdom_dokument_journalpost_dokument_info_id_key;

alter table SYKDOM_DOKUMENT_HAR_OPPDATERT_EKSISTERENDE_VURDERINGER rename to PLEIETRENGENDE_SYKDOM_DOKUMENT_HAR_OPPDATERT_VURDERINGER;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT_HAR_OPPDATERT_VURDERINGER rename column sykdom_dokument_id to pleietrengende_sykdom_dokument_id;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT_HAR_OPPDATERT_VURDERINGER rename constraint sykdom_dokument_har_oppdatert_eksisterende_vurderinger_pkey to pleietrengende_sykdom_dokument_har_oppdatert_vurderinger_pkey;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT_HAR_OPPDATERT_VURDERINGER rename constraint fk_sykdom_dokument_har_oppdatert_eksisterende_vurderinger_01 to fk_pleietrengende_sykdom_dokument_har_oppdatert_vurderinger_01;

alter table SYKDOM_DOKUMENT_INFORMASJON rename to PLEIETRENGENDE_SYKDOM_DOKUMENT_INFORMASJON;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT_INFORMASJON rename column sykdom_dokument_id to pleietrengende_sykdom_dokument_id;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT_INFORMASJON rename column duplikat_av_sykdom_dokument_id to duplikat_av_pleietrengende_sykdom_dokument_id;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT_INFORMASJON rename constraint sykdom_dokument_informasjon_pkey to pleietrengende_sykdom_dokument_informasjon_pkey;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT_INFORMASJON rename constraint sykdom_dokument_informasjon_sykdom_dokument_id_versjon_key to pleietrengende_sykdom_dokument_informasjon_dok_id_versjon_key;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT_INFORMASJON rename constraint fk_sykdom_dokument_informasjon to fk_pleietrengende_sykdom_dokument_informasjon_01;
alter table PLEIETRENGENDE_SYKDOM_DOKUMENT_INFORMASJON add constraint fk_pleietrengende_sykdom_dokument_informasjon_02 foreign key (duplikat_av_pleietrengende_sykdom_dokument_id) references pleietrengende_sykdom_dokument_informasjon(id);

-- grunnlag
alter table SYKDOM_GRUNNLAG_BEHANDLING rename to GR_MEDISINSK;
alter table GR_MEDISINSK rename column sykdom_grunnlag_id to medisinsk_grunnlagsdata_id;
alter table GR_MEDISINSK rename constraint sykdom_grunnlag_behandling_pkey to gr_medisinsk_pkey;
alter table GR_MEDISINSK rename constraint sykdom_grunnlag_behandling_behandling_uuid_versjon_key to gr_medisinsk_id_behandling_uuid_versjon_key;
alter table GR_MEDISINSK rename constraint sykdom_grunnlag_behandling_saksnummer_behandlingsnummer_ver_key to gr_medisinsk_id_saksnummer_behandlingsnummer_ver_key;
alter table GR_MEDISINSK rename constraint fk_sykdom_grunnlag_behandling_01 to fk_gr_medisinsk_01;
alter table GR_MEDISINSK rename constraint fk_sykdom_grunnlag_behandling_02 to fk_gr_medisinsk_02;
alter table GR_MEDISINSK rename constraint fk_sykdom_grunnlag_behandling_03 to fk_gr_medisinsk_03;
alter table GR_MEDISINSK add constraint fk_gr_medisinsk_04 foreign key (behandling_uuid) references behandling(uuid);

-- grunnlagsdata
alter table SYKDOM_GRUNNLAG rename to MEDISINSK_GRUNNLAGSDATA;
alter table MEDISINSK_GRUNNLAGSDATA rename column sykdom_grunnlag_uuid to sporingsreferanse;
alter table MEDISINSK_GRUNNLAGSDATA rename column sykdom_innleggelser_id to pleietrengende_sykdom_innleggelser_id;
alter table MEDISINSK_GRUNNLAGSDATA rename column sykdom_diagnosekoder_id to pleietrengende_sykdom_diagnoser_id;
alter table MEDISINSK_GRUNNLAGSDATA rename constraint sykdom_grunnlag_pkey to medisinsk_grunnlagsdata_pkey;
alter table MEDISINSK_GRUNNLAGSDATA rename constraint sykdom_grunnlag_sykdom_grunnlag_uuid_key to medisinsk_grunnlagsdata_uuid_key;
alter table MEDISINSK_GRUNNLAGSDATA rename constraint fk_sykdom_grunnlag_01 to fk_medisinsk_grunnlagsdata_01;
alter table MEDISINSK_GRUNNLAGSDATA rename constraint fk_sykdom_grunnlag_02 to fk_medisinsk_grunnlagsdata_02;

-- koblinger
alter table SYKDOM_VURDERING_VERSJON_DOKUMENT rename to PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_ANVENDT_DOKUMENT;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_ANVENDT_DOKUMENT rename column sykdom_dokument_id to pleietrengende_sykdom_dokument_id;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_ANVENDT_DOKUMENT rename column sykdom_vurdering_versjon_id to pleietrengende_sykdom_vurdering_versjon_id;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_ANVENDT_DOKUMENT rename constraint sykdom_vurdering_versjon_doku_sykdom_dokument_id_sykdom_vur_key to pleietrengende_sykdom_vurdering_versjon_dokument_id_vv_key;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_ANVENDT_DOKUMENT rename constraint fk_sykdom_vurdering_versjon_dokument_01 to pleietrengende_sykdom_vurdering_versjon_dokument_01;
alter table PLEIETRENGENDE_SYKDOM_VURDERING_VERSJON_ANVENDT_DOKUMENT rename constraint fk_sykdom_vurdering_versjon_dokument_02 to pleietrengende_sykdom_vurdering_versjon_dokument_02;

alter table SYKDOM_GRUNNLAG_DOKUMENT rename to MEDISINSK_GRUNNLAGSDATA_GODKJENTE_LEGEERKLAERINGER;
alter table MEDISINSK_GRUNNLAGSDATA_GODKJENTE_LEGEERKLAERINGER rename column sykdom_grunnlag_id to medisinsk_grunnlagsdata_id;
alter table MEDISINSK_GRUNNLAGSDATA_GODKJENTE_LEGEERKLAERINGER rename column sykdom_dokument_id to pleietrengende_sykdom_dokument_id;
alter table MEDISINSK_GRUNNLAGSDATA_GODKJENTE_LEGEERKLAERINGER rename constraint sykdom_grunnlag_dokument_sykdom_grunnlag_id_sykdom_dokument_key to medisinsk_grunnlagsdata_godkj_legeerklaeringer_grlg_dok_key;
alter table MEDISINSK_GRUNNLAGSDATA_GODKJENTE_LEGEERKLAERINGER rename constraint fk_sykdom_grunnlag_dokument_01 to fk_medisinsk_grunnlagsdata_godkjente_legeerklaeringer_01;
alter table MEDISINSK_GRUNNLAGSDATA_GODKJENTE_LEGEERKLAERINGER rename constraint fk_sykdom_grunnlag_dokument_02 to fk_medisinsk_grunnlagsdata_godkjente_legeerklaeringer_02;

alter table SYKDOM_GRUNNLAG_VURDERING rename to MEDISINSK_GRUNNLAGSDATA_GJELDENDE_VURDERINGER;
alter table MEDISINSK_GRUNNLAGSDATA_GJELDENDE_VURDERINGER rename column sykdom_grunnlag_id to medisinsk_grunnlagsdata_id;
alter table MEDISINSK_GRUNNLAGSDATA_GJELDENDE_VURDERINGER rename column sykdom_vurdering_versjon_id to pleietrengende_sykdom_vurdering_versjon_id;
alter table MEDISINSK_GRUNNLAGSDATA_GJELDENDE_VURDERINGER rename constraint sykdom_grunnlag_vurdering_sykdom_grunnlag_id_sykdom_vurderi_key to medisinsk_grunnlagsdata_gjeldende_vurderinger_grunnlag_vv_key;
alter table MEDISINSK_GRUNNLAGSDATA_GJELDENDE_VURDERINGER rename constraint fk_sykdom_grunnlag_vurdering_01 to fk_medisinsk_grunnlagsdata_gjeldende_vurderinger_01;
alter table MEDISINSK_GRUNNLAGSDATA_GJELDENDE_VURDERINGER rename constraint fk_sykdom_grunnlag_vurdering_02 to fk_medisinsk_grunnlagsdata_gjeldende_vurderinger_02;
