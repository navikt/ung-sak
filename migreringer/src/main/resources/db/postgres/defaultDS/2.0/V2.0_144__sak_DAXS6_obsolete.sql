-- PLS uten søknad som har blitt migrert til en annen sak. Skal henlegges. 
update fagsak set ytelse_type='OBSOLETE' where saksnummer = 'DAXS6';
