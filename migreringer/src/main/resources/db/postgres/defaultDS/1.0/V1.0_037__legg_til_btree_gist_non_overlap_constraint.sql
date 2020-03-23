-- avhenger av at følgende er gjort på db : "create extension if not exists btree_gist;" som superbruker

alter table fagsak add constraint fagsak_ikke_overlapp_periode EXCLUDE USING GIST (
	    ytelse_type WITH =,
        bruker_aktoer_id WITH =,
        pleietrengende_aktoer_id WITH =,
        periode WITH &&
    );	
