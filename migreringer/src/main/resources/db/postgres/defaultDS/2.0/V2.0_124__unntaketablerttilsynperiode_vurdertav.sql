alter table psb_unntak_etablert_tilsyn_periode add column vurdert_av varchar(20);
alter table psb_unntak_etablert_tilsyn_periode add column vurdert_tid timestamp(3);

update psb_unntak_etablert_tilsyn_periode it set vurdert_av = (select opprettet_av from psb_unntak_etablert_tilsyn_periode where fom = it.fom and tom = it.tom and begrunnelse = it.begrunnelse and resultat = it.resultat and soeker_aktoer_id = it.soeker_aktoer_id and kilde_behandling_id = it.kilde_behandling_id order by opprettet_tid limit 1),
                                                 vurdert_tid = (select opprettet_tid from psb_unntak_etablert_tilsyn_periode where fom = it.fom and tom = it.tom and begrunnelse = it.begrunnelse and resultat = it.resultat and soeker_aktoer_id = it.soeker_aktoer_id and kilde_behandling_id = it.kilde_behandling_id order by opprettet_tid limit 1);

alter table psb_unntak_etablert_tilsyn_periode alter column vurdert_av set not null;
alter table psb_unntak_etablert_tilsyn_periode alter column vurdert_tid set not null;
