-- Videre opprydding av duplikatbehandling. Oppryddingen fra forrige skript skulle ha fjernet behandlingsvedtak.
-- Saksnummer 65S3S FRISINN

-- Fjern vedtak på henlagt behandling
delete from behandling_vedtak where behandling_id = 1000402;;

-- Knytt behandling for ny søknadsperiode til riktig førstegangsbehandling
update behandling_arsak set original_behandling_id = 1007215 where behandling_id = 1086309;
