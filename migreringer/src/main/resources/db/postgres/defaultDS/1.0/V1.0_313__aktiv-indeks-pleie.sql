CREATE UNIQUE INDEX IF NOT EXISTS uidx_rs_pleiebehov_02 ON rs_pleiebehov (behandling_id) WHERE (aktiv = true);
