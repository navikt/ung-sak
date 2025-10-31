alter table PART add column identifikasjon_type_kode varchar(50);
update PART set identifikasjon_type_kode='ORGNR' where identifikasjon_type = '0';
update PART set identifikasjon_type_kode='AKTØRID' where identifikasjon_type = '1';
update PART set identifikasjon_type_kode='FNR' where identifikasjon_type = '2';
alter table PART alter column identifikasjon_type_kode set not null;
alter table PART alter column identifikasjon_type drop not null;

alter table PART add column rolle_type_kode varchar(50);
update PART set rolle_type_kode='ARBEIDSGIVER' where rolle_type = '0';
update PART set rolle_type_kode='BRUKER' where rolle_type = '1';
alter table PART alter column rolle_type_kode set not null;
alter table PART alter column rolle_type drop not null;

alter table ETTERLYSNING add column etterlysning_type varchar(50);
update ETTERLYSNING set etterlysning_type='UTTALELSE_KONTROLL_INNTEKT' where type = '0';
update ETTERLYSNING set etterlysning_type='UTTALELSE_ENDRET_STARTDATO' where type = '1';
update ETTERLYSNING set etterlysning_type='UTTALELSE_ENDRET_SLUTTDATO' where type = '2';
alter table ETTERLYSNING alter column etterlysning_type set not null;
alter table ETTERLYSNING alter column type drop not null;


--det har vært endringer i enum-verder, men det var før lansering
alter table ETTERLYSNING add column etterlysning_status varchar(50);
update ETTERLYSNING set etterlysning_status='OPPRETTET' where status = '0';
update ETTERLYSNING set etterlysning_status='VENTER' where status = '1';
update ETTERLYSNING set etterlysning_status='MOTTATT_SVAR' where status = '2';
update ETTERLYSNING set etterlysning_status='SKAL_AVBRYTES' where status = '3';
update ETTERLYSNING set etterlysning_status='AVBRUTT' where status = '4';
update ETTERLYSNING set etterlysning_status='UTLOPT' where status = '5';
alter table ETTERLYSNING alter column etterlysning_status set not null;
alter table ETTERLYSNING alter column status drop not null;


alter table UTTALELSE_V2 add column endring_type_kode varchar(50);
update UTTALELSE_V2 set endring_type_kode='ENDRET_INNTEKT' where endring_type = '0';
update UTTALELSE_V2 set endring_type_kode='ENDRET_STARTDATO' where endring_type = '1';
update UTTALELSE_V2 set endring_type_kode='ENDRET_SLUTTDATO' where endring_type = '2';
alter table UTTALELSE_V2 alter column endring_type_kode set not null;
alter table UTTALELSE_V2 alter column endring_type drop not null;
