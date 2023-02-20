-- TSF-2566 Saksnummer 8DSJM (OMP)
--setter resultat_type til en HENLAGT-type

update behandling set
    behandling_resultat_type = 'HENLAGT_FEILOPPRETTET',
    sist_oppdatert_tidspunkt=current_timestamp at time zone 'UTC'
where id = 1227836 and uuid = '2fcb2c4d-9537-48bb-80fe-73d250a862ec';
