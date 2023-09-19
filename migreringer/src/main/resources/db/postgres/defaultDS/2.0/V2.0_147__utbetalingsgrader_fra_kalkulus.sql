ALTER TABLE BR_PERIODE ADD total_utbetalingsgrad_fra_uttak NUMERIC (19,4);
comment on column BR_PERIODE.total_utbetalingsgrad_fra_uttak is 'Total utbetalingsgrad fra uttak. Utregnet separat fra reduksjon ved tilkommet inntekt.';

ALTER TABLE BR_PERIODE ADD total_utbetalingsgrad_etter_reduksjon_ved_tilkommet_inntekt NUMERIC (19,4);
comment on column BR_PERIODE.total_utbetalingsgrad_etter_reduksjon_ved_tilkommet_inntekt is 'Total utbetalingsgrad etter reduksjon ved tilkommet inntekt. Utregnet separat fra utbetalingsgrad fra uttak.';
