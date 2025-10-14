create table GR_KONTROLLERT_INNTEKT
(
    ID                     bigint                               NOT NULL PRIMARY KEY,
    BEHANDLING_ID          bigint REFERENCES BEHANDLING (id)    NOT NULL,
    KONTROLLERT_INNTEKT_PERIODER_ID          bigint REFERENCES KONTROLLERT_INNTEKT_PERIODER (id)    NOT NULL,
    VERSJON                bigint       default 0               NOT NULL,
    AKTIV                  boolean      default true            NOT NULL,
    OPPRETTET_AV           VARCHAR(20)  default 'VL'            NOT NULL,
    OPPRETTET_TID          TIMESTAMP(3) default localtimestamp  NOT NULL,
    ENDRET_AV              VARCHAR(20),
    ENDRET_TID             TIMESTAMP(3)
);
create index IDX_GR_KONTROLLERT_INNTEKT_BEHANDLING on GR_KONTROLLERT_INNTEKT (BEHANDLING_ID);
create index IDX_GR_KONTROLLERT_INNTEKT_PERIODER on GR_KONTROLLERT_INNTEKT (KONTROLLERT_INNTEKT_PERIODER_ID);
CREATE UNIQUE INDEX UIDX_GR_KONTROLLERT_INNTEKT_AKTIV_BEHANDLING ON GR_KONTROLLERT_INNTEKT (BEHANDLING_ID) WHERE (AKTIV = TRUE);
create sequence if not exists SEQ_GR_KONTROLLERT_INNTEKT increment by 50 minvalue 1000000;


insert into GR_KONTROLLERT_INNTEKT (id, behandling_id, KONTROLLERT_INNTEKT_PERIODER_ID)
select nextval('SEQ_GR_KONTROLLERT_INNTEKT'), p.behandling_id, p.id from (
                                                                                        select distinct perioder.behandling_id, perioder.id
                                                                                        from KONTROLLERT_INNTEKT_PERIODER perioder
                                                                                        where perioder.aktiv = true
                                                                                    ) p;
