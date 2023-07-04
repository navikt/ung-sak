--se TSF-3238
--fjerner KS-sak uten behandling - det finnes en annen fagsak som skal gjelde
update fagsak set ytelse_type='OBSOLETE' where saksnummer ='D725S';

