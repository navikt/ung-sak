DELETE FROM KONTROLLERT_INNTEKT_PERIODE periode where
    periode.KONTROLLERT_INNTEKT_PERIODER_ID in (select perioder.id from KONTROLLERT_INNTEKT_PERIODER perioder where
                                             aktiv = false
and not exists (Select 1 from GR_KONTROLLERT_INNTEKT gk where gk.KONTROLLERT_INNTEKT_PERIODER_ID = perioder.id));


DELETE FROM KONTROLLERT_INNTEKT_PERIODER perioder where
                                             aktiv = false
and not exists (Select 1 from GR_KONTROLLERT_INNTEKT gk where gk.KONTROLLERT_INNTEKT_PERIODER_ID = perioder.id);

drop index idx_kontrollert_inntekt_perioder_behandling_id;

create unique index idx_kontrollert_inntekt_perioder_aktiv_behandling_id
    on kontrollert_inntekt_perioder (behandling_id)
    where aktiv = true;

-- Drop the unique index before dropping the column
drop index idx_kontrollert_inntekt_perioder_aktiv_behandling_id;

ALTER table KONTROLLERT_INNTEKT_PERIODER
DROP COLUMN BEHANDLING_ID;
ALTER table KONTROLLERT_INNTEKT_PERIODER
DROP COLUMN AKTIV;
