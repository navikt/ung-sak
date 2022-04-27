-- TSF-2566
-- Saksnummer 8DSJM (OMP)

-- Hard avslutning
update behandling set behandling_status='AVSLU', SIST_OPPDATERT_TIDSPUNKT=current_timestamp at time zone 'UTC'
where fagsak_id IN (select f.id from fagsak f where f.saksnummer IN ('8DSJM') AND  behandling_status NOT IN ('AVSLU'))
  and id = 1227836;
