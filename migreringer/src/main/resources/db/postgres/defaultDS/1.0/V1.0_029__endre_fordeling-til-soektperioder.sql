drop sequence SEQ_GR_FORDELING;

alter sequence SEQ_FO_FORDELING rename to SEQ_UT_SOEKNADSPERIODER;
alter sequence SEQ_FO_FORDELING_PERIODE rename to SEQ_UT_SOEKNADSPERIODE;

alter table FO_FORDELING_PERIODE rename to UT_SOEKNADSPERIODE;
alter table FO_FORDELING rename to UT_SOEKNADSPERIODER;

alter table GR_UTTAK 
  add column fastsatt_uttak_id bigint,
  add column soeknadsperioder_id bigint;

alter table GR_UTTAK alter column oppgitt_uttak_id DROP NOT NULL;

alter table UT_SOEKNADSPERIODE
  add column soeknadsperioder_id bigint;
update UT_SOEKNADSPERIODE set soeknadsperioder_id = fordeling_id;
alter table UT_SOEKNADSPERIODE drop column fordeling_id;
alter table UT_SOEKNADSPERIODE alter column soeknadsperioder_id SET NOT NULL;
alter table UT_SOEKNADSPERIODE add constraint FK_UT_SOEKNADSPERIODE_01 FOREIGN KEY (soeknadsperioder_id) REFERENCES UT_SOEKNADSPERIODER(id);

update GR_UTTAK grut set soeknadsperioder_id = (select grf.oppgitt_fordeling_id from GR_FORDELING grf where grf.behandling_id= grut.behandling_id);
alter table GR_UTTAK add constraint FK_GR_UTTAK_03 FOREIGN KEY (soeknadsperioder_id) REFERENCES UT_SOEKNADSPERIODER(id);

alter table GR_UTTAK add constraint FK_GR_UTTAK_04 FOREIGN KEY (fastsatt_uttak_id) REFERENCES UT_UTTAK(id);

drop table GR_FORDELING cascade;
