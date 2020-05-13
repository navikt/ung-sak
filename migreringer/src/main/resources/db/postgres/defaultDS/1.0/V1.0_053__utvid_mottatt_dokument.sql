ALTER TABLE MOTTATT_DOKUMENT
    ALTER COLUMN dokument_kategori DROP NOT NULL;

update MOTTATT_DOKUMENT set type='INNTEKTSMELDING'; --alle innslag hittil er inntektsmelinger, så ok.
    
alter table MOTTATT_DOKUMENT alter column payload TYPE oid using (payload::oid);