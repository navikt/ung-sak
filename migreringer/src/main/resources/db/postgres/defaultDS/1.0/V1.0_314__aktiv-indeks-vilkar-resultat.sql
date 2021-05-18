CREATE UNIQUE INDEX IF NOT EXISTS uidx_rs_vilkars_resultat_02 ON rs_vilkars_resultat (behandling_id) WHERE (aktiv = true);
