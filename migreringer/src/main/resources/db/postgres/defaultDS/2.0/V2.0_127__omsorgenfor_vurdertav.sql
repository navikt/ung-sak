alter table omsorgen_for_periode add column vurdert_av varchar(20);
alter table omsorgen_for_periode add column vurdert_tid timestamp(3);

update omsorgen_for_periode it set vurdert_av = (select ofp.opprettet_av from omsorgen_for_periode ofp
                                                                         join gr_omsorgen_for gof on ofp.omsorgen_for_id = gof.omsorgen_for_id
                                                                         where ofp.fom = it.fom and ofp.tom = it.tom and ofp.begrunnelse is not distinct from it.begrunnelse and ofp.resultat = it.resultat and ofp.relasjon is not distinct from it.relasjon and ofp.relasjonsbeskrivelse is not distinct from it.relasjonsbeskrivelse
                                                                            and gof.behandling_id = (select behandling_id from gr_omsorgen_for where omsorgen_for_id = it.omsorgen_for_id)
                                                                         order by ofp.opprettet_tid limit 1),
                                   vurdert_tid = (select ofp.opprettet_tid from omsorgen_for_periode ofp
                                                                           join gr_omsorgen_for gof on ofp.omsorgen_for_id = gof.omsorgen_for_id
                                                                           where ofp.fom = it.fom and ofp.tom = it.tom and ofp.begrunnelse is not distinct from it.begrunnelse and ofp.resultat = it.resultat and ofp.relasjon is not distinct from it.relasjon and ofp.relasjonsbeskrivelse is not distinct from it.relasjonsbeskrivelse
                                                                              and gof.behandling_id = (select behandling_id from gr_omsorgen_for where omsorgen_for_id = it.omsorgen_for_id)
                                                                           order by ofp.opprettet_tid limit 1);

alter table omsorgen_for_periode alter column vurdert_av set not null;
alter table omsorgen_for_periode alter column vurdert_tid set not null;
