create table if not exists SYKDOM_DOKUMENT_HAR_OPPDATERT_EKSISTERENDE_VURDERINGER
(
    SYKDOM_DOKUMENT_ID     BIGINT                  NOT NULL PRIMARY KEY,

    OPPRETTET_AV               VARCHAR(20)                 NOT NULL,
    OPPRETTET_TID              TIMESTAMP(3)                NOT NULL,
    CONSTRAINT FK_SYKDOM_DOKUMENT_HAR_OPPDATERT_EKSISTERENDE_VURDERINGER_01
        FOREIGN KEY(SYKDOM_DOKUMENT_ID) REFERENCES SYKDOM_DOKUMENT(ID)
);

insert into SYKDOM_DOKUMENT_HAR_OPPDATERT_EKSISTERENDE_VURDERINGER(sykdom_dokument_id, opprettet_av, opprettet_tid)
select id, 'konvertering', now()
from sykdom_dokument
