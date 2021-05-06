create table if not exists  TMP_AKTOER_ID(ID BIGINT NOT NULL,
AKTOER_ID VARCHAR(50) NOT NULL,
IDENT VARCHAR(50),
IDENT_TYPE VARCHAR(20) DEFAULT 'FNR' NOT NULL,
VERSJON BIGINT DEFAULT 0 NOT NULL,
OPPRETTET_AV VARCHAR(20) DEFAULT 'VL' NOT NULL,
OPPRETTET_TID TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP NOT NULL,
ENDRET_AV VARCHAR(20),
ENDRET_TID TIMESTAMP(3));

create index IDX_TMP_AKTOER_ID_01 on TMP_AKTOER_ID (AKTOER_ID);
create index IDX_TMP_AKTOER_ID_02 on TMP_AKTOER_ID (IDENT, IDENT_TYPE);

create unique index UIDX_TMP_AKTOER_ID_01 ON TMP_AKTOER_ID (aktoer_id, ident, ident_type);


Insert into PROSESS_TASK_TYPE (KODE,NAVN,FEIL_MAKS_FORSOEK,FEIL_SEK_MELLOM_FORSOEK,FEILHANDTERING_ALGORITME,BESKRIVELSE,CRON_EXPRESSION) 
values ('rapportering.identCache','AktørId - FNR cache',20,30,'DEFAULT','Trigger temporær caching av FNR (til bruk i rapportering)', null);

insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
values (nextval('seq_prosess_task'), 'rapportering.identCache', nextval('seq_prosess_task_gruppe'), null, null);

