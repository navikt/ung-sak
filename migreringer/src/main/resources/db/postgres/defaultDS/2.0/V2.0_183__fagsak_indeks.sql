create index idx_fagsak_4 on fagsak using gist (pleietrengende_aktoer_id, ytelse_type, periode) WHERE (pleietrengende_aktoer_id IS NOT NULL AND periode IS NOT NULL);
