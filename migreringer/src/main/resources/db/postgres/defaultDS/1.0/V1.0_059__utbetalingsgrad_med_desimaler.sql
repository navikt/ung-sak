ALTER TABLE TILKJENT_YTELSE_PERIODE
    ALTER COLUMN utbetalingsgrad TYPE numeric(13,10);

ALTER TABLE TILKJENT_YTELSE_PERIODE
    ADD CONSTRAINT chk_utbetalingsgrad_range CHECK (utbetalingsgrad >= 0 AND utbetalingsgrad <= 100);

