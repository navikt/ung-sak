ALTER table KONTROLLERT_INNTEKT_PERIODE
    ADD COLUMN rapportert_inntekt_pr_mnd numeric;
ALTER table KONTROLLERT_INNTEKT_PERIODE
    ADD COLUMN register_inntekt_pr_mnd numeric;

UPDATE KONTROLLERT_INNTEKT_PERIODE set rapportert_inntekt_pr_mnd = rapportert_inntekt where rapportert_inntekt is not null;
UPDATE KONTROLLERT_INNTEKT_PERIODE set register_inntekt_pr_mnd = register_inntekt where register_inntekt is not null;
