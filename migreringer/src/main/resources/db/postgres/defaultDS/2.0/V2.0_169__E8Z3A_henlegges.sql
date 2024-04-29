--se FAGSYSTEM-314419
update behandling set behandling_status='AVSLU' where id = 1740589 and fagsak_id = 1335498;
update fagsak set ytelse_type='OBSOLETE' where saksnummer = 'E8Z3A' and id = 1335498;
