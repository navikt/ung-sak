ALTER TABLE gr_bosatt_avklaring
    ADD COLUMN soknad_avklaring_holder_id BIGINT REFERENCES bosatt_avklaring_holder (id);

COMMENT ON COLUMN gr_bosatt_avklaring.soknad_avklaring_holder_id IS 'Bostedsopplysninger oppgitt av bruker i søknaden.';

ALTER TABLE akt_soekt_periode DROP COLUMN IF EXISTS er_bosatt_i_trondheim;
