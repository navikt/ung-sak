UPDATE vr_vilkar_periode
SET utfall = 'OPPFYLT'
WHERE utfall = 'IKKE_VURDERT'
  and vilkar_id IN (SELECT vv.id
                    from vr_vilkar vv
                             INNER JOIN rs_vilkars_resultat rv on vv.vilkar_resultat_id = rv.vilkarene_id
                             INNER JOIN behandling b on rv.behandling_id = b.id
                             INNER JOIN fagsak f on b.fagsak_id = f.id
                    WHERE f.saksnummer in ('CTXZ6',
                                           'CTZBS',
                                           'CUDWS',
                                           'CUGSY',
                                           'CV0XY',
                                           'CVT12',
                                           'CVT3U',
                                           'CW6SW',
                                           'CW7M2',
                                           'CWABA',
                                           'CWARY')
                      and rv.aktiv = true
                      and vv.vilkar_type = 'FP_VK_1');
