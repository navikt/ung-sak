alter table fagsak 	add column gjelder_fom date,
					add column gjelder_tom date;
					
alter table fagsak ADD CONSTRAINT fagsak_valid_range_check CHECK ((gjelder_fom is null and gjelder_tom is null) OR (gjelder_tom >= gjelder_fom));

update fagsak f set 
  gjelder_fom = (select min(usp.fom) from UT_SOEKNADSPERIODE usp INNER JOIN GR_UTTAK gr ON gr.soeknadsperioder_id = usp.soeknadsperioder_id INNER JOIN BEHANDLING b on b.id=gr.behandling_id WHERE b.fagsak_id=f.id)
, gjelder_tom = (select max(usp.tom) from UT_SOEKNADSPERIODE usp INNER JOIN GR_UTTAK gr ON gr.soeknadsperioder_id = usp.soeknadsperioder_id INNER JOIN BEHANDLING b on b.id=gr.behandling_id WHERE b.fagsak_id=f.id);

