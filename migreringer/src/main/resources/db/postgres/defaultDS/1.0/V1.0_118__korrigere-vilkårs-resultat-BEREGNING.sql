UPDATE vr_vilkar_periode
SET utfall = 'IKKE_OPPFYLT'
WHERE utfall = 'IKKE_VURDERT'
  and vilkar_id IN (SELECT vv.id
                    from vr_vilkar vv
                             INNER JOIN rs_vilkars_resultat rv on vv.vilkar_resultat_id = rv.vilkarene_id
                             INNER JOIN behandling b on rv.behandling_id = b.id
                             INNER JOIN fagsak f on b.fagsak_id = f.id
                    WHERE f.saksnummer in
                          ('6PGBM', '6P0B8', '6SNTY', '6oJU6', '725T2', '6P522', '6iW68', '6oZ5K', '6J7Z8', '6oZ9Q',
                           '6P3V0', '6K4B4', '6V7M4', '7R75o', '758AU', '6PCGQ', '6PBRQ', '6JPAU', '78Xo8', '6K2Yi',
                           '7N02S', '6PBGM')
                      and rv.aktiv = true
                      and vv.vilkar_type = 'FP_VK_41');
