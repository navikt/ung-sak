
alter table fagsak drop constraint if exists fagsak_ikke_overlapp_periode cascade;
alter table fagsak drop column if exists gjelder_fom cascade, 
				   drop column if exists gjelder_tom cascade;

alter table fagsak add constraint unik_fagsak_1 EXCLUDE USING GIST (
	    ytelse_type WITH =,
        bruker_aktoer_id WITH =,
        pleietrengende_aktoer_id WITH =,
        periode WITH &&
    )
    WHERE (pleietrengende_aktoer_id IS NOT NULL AND periode IS NOT NULL);	
    
alter table fagsak add constraint unik_fagsak_2 EXCLUDE USING GIST (
	    ytelse_type WITH =,
        bruker_aktoer_id WITH =,
        periode WITH &&
    )
    WHERE (pleietrengende_aktoer_id IS NULL AND periode IS NOT NULL);	

CREATE UNIQUE INDEX UIDX_FAGSAK_3 ON FAGSAK( ytelse_type, bruker_aktoer_id)
    WHERE (pleietrengende_aktoer_id IS NULL AND periode IS NULL);	

