ALTER TABLE KONTROLLERT_INNTEKT_PERIODE
    ADD COLUMN ytelse numeric;

COMMENT ON COLUMN kontrollert_inntekt_periode.ytelse IS 'Ytelse som bruker har hatt i den kontrollerte perioden.';

