UPDATE vr_vilkar_periode
SET utfall = 'IKKE_OPPFYLT'
WHERE utfall = 'IKKE_VURDERT'
  and vilkar_id IN (SELECT vv.id
                    from vr_vilkar vv
                             INNER JOIN rs_vilkars_resultat rv on vv.vilkar_resultat_id = rv.vilkarene_id
                             INNER JOIN behandling b on rv.behandling_id = b.id
                             INNER JOIN fagsak f on b.fagsak_id = f.id
                    WHERE f.saksnummer in ('6GMJC',
                                           '6GN46',
                                           '6K5GS',
                                           '6L3YG',
                                           '6MXYU',
                                           '6o4LK',
                                           '6oA00',
                                           '6oNMA',
                                           '6PQ8K',
                                           '6QLES',
                                           '6RDoU',
                                           '6S5FG',
                                           '6SB0U',
                                           '6SHFE',
                                           '6TRJ4',
                                           '6ULW6',
                                           '6V0LC',
                                           '6VE3G',
                                           '6W0S4',
                                           '6W2XW',
                                           '6WPXo',
                                           '6XENY',
                                           '6XG8W',
                                           '6Z9U0',
                                           '72NA8',
                                           '732K8',
                                           '73X5M',
                                           '76YLW',
                                           '776SC',
                                           '77HL8',
                                           '78FQE',
                                           '7C290',
                                           '7DDiE',
                                           '7DYJC',
                                           '7GDRM',
                                           '7GoQ2',
                                           '7oiS8',
                                           '7PFJE',
                                           '7PFRQ',
                                           '7PFT4',
                                           '7PGYS',
                                           '7RBH8')
                      and rv.aktiv = true
                      and vv.vilkar_type = 'FP_VK_41');
