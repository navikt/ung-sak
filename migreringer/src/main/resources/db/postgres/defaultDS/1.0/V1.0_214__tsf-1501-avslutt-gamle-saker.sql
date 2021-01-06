-- marker gamle saker som avsluttet (følgefeil av V1.0_173)

-- "saksnummer" "behandling_id"
-- "8ViQM" 1208599
-- "7VPQG" 1213077
-- "732Zi" 1207694
-- "8HREo" 1220236


update behandling_steg_tilstand set behandling_steg_status='UTFØRT' 
where behandling_steg='IVEDSTEG' and behandling_id in (1208599, 1213077, 1207694, 1220236) and aktiv=true;

update behandling set behandling_status='AVSLU'
where behandling_status='IVED' and id in (1208599, 1213077, 1207694, 1220236);

update fagsak f set fagsak_status = 'AVSLU'
where saksnummer in ('8ViQM', '8ViQM', '732Zi', '8HREo')
and not exists (select 1 from behandling b where b.fagsak_id=f.id and b.behandling_status!='AVSLU');
;