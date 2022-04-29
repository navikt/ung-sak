-- TSF-2566 Saksnummer 8DSJM (OMP)

--siste behandling peker til en behandling som ikke er ordentlig avsluttet - i praksis henlagt
--må peke til tidligere behandling for å kunne fullføre saksbehandling
update behandling set original_behandling_id = 1132872, sist_oppdatert_tidspunkt=current_timestamp at time zone 'UTC'  where id = 1544012 and uuid = '3f4f2dc0-3685-4a4c-b021-260c59d4a707';

