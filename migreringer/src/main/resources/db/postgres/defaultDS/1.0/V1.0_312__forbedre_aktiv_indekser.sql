
CREATE UNIQUE INDEX IF NOT EXISTS uidx_gr_omsorgen_for_02 ON gr_omsorgen_for (behandling_id) WHERE (aktiv = true);

 