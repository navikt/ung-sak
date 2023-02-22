alter table psb_unntak_etablert_tilsyn_periode add column vurdert_av varchar(20);
alter table psb_unntak_etablert_tilsyn_periode add column vurdert_tid timestamp(3);

with eldste_rader as (select distinct on (fom, tom, begrunnelse, resultat, soeker_aktoer_id, kilde_behandling_id) * from psb_unntak_etablert_tilsyn_periode order by fom, tom, begrunnelse, resultat, soeker_aktoer_id, kilde_behandling_id, opprettet_tid)
update psb_unntak_etablert_tilsyn_periode it set vurdert_av = (select eldste_rader.opprettet_av from eldste_rader where eldste_rader.fom = it.fom and eldste_rader.tom = it.tom and eldste_rader.begrunnelse = it.begrunnelse and eldste_rader.resultat = it.resultat and eldste_rader.soeker_aktoer_id = it.soeker_aktoer_id and eldste_rader.kilde_behandling_id = it.kilde_behandling_id),
                                                 vurdert_tid = (select eldste_rader.opprettet_tid from eldste_rader where eldste_rader.fom = it.fom and eldste_rader.tom = it.tom and eldste_rader.begrunnelse = it.begrunnelse and eldste_rader.resultat = it.resultat and eldste_rader.soeker_aktoer_id = it.soeker_aktoer_id and eldste_rader.kilde_behandling_id = it.kilde_behandling_id);

alter table psb_unntak_etablert_tilsyn_periode alter column vurdert_av set not null;
alter table psb_unntak_etablert_tilsyn_periode alter column vurdert_tid set not null;
