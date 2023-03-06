alter table medlemskap_vurdering_lopende add column vurdert_av varchar(20);
alter table medlemskap_vurdering_lopende add column vurdert_tid timestamp(3);

update medlemskap_vurdering_lopende it set vurdert_av = (select mvl.opprettet_av from medlemskap_vurdering_lopende mvl
                                                                                 join gr_medlemskap gm on mvl.vurdert_periode_id = gm.vurdering_lopende_id
                                                                                 where mvl.oppholdsrett_vurdering is not distinct from it.oppholdsrett_vurdering and mvl.lovlig_opphold_vurdering is not distinct from it.lovlig_opphold_vurdering and mvl.bosatt_vurdering is not distinct from it.bosatt_vurdering and mvl.er_eos_borger is not distinct from it.er_eos_borger and mvl.vurderingsdato = it.vurderingsdato and mvl.begrunnelse is not distinct from it.begrunnelse and mvl.manuell_vurd = it.manuell_vurd
                                                                                    and gm.behandling_id = (select behandling_id from gr_medlemskap where gm.vurdering_lopende_id = it.vurdert_periode_id)
                                                                                 order by mvl.opprettet_tid limit 1),
                                           vurdert_tid = (select mvl.opprettet_tid from medlemskap_vurdering_lopende mvl
                                                                                   join gr_medlemskap gm on mvl.vurdert_periode_id = gm.vurdering_lopende_id
                                                                                   where mvl.oppholdsrett_vurdering is not distinct from it.oppholdsrett_vurdering and mvl.lovlig_opphold_vurdering is not distinct from it.lovlig_opphold_vurdering and mvl.bosatt_vurdering is not distinct from it.bosatt_vurdering and mvl.er_eos_borger is not distinct from it.er_eos_borger and mvl.vurderingsdato = it.vurderingsdato and mvl.begrunnelse is not distinct from it.begrunnelse and mvl.manuell_vurd = it.manuell_vurd
                                                                                     and gm.behandling_id = (select behandling_id from gr_medlemskap where gm.vurdering_lopende_id = it.vurdert_periode_id)
                                                                                   order by mvl.opprettet_tid limit 1);

alter table medlemskap_vurdering_lopende alter column vurdert_av set not null;
alter table medlemskap_vurdering_lopende alter column vurdert_tid set not null;
