alter table SO_SOEKNAD add column fom date,
                       add column tom date;
alter table so_soeknad add constraint chk_so_soeknad_fom_tom check ((fom is null and tom is null) OR fom <= tom);

update SO_SOEKNAD s set 
	fom = (select usp.fom from UT_SOEKNADSPERIODE usp INNER JOIN GR_UTTAK gr ON gr.soeknadsperioder_id = usp.soeknadsperioder_id INNER JOIN GR_SOEKNAD grs ON grs.behandling_id=gr.behandling_id where grs.aktiv=true and gr.aktiv = true and grs.soeknad_id=s.id),
  	tom = (select usp.tom from UT_SOEKNADSPERIODE usp INNER JOIN GR_UTTAK gr ON gr.soeknadsperioder_id = usp.soeknadsperioder_id INNER JOIN GR_SOEKNAD grs ON grs.behandling_id=gr.behandling_id where grs.aktiv=true and gr.aktiv = true and grs.soeknad_id=s.id);