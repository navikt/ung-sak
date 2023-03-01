UPDATE vr_vilkar_periode
SET utfall = 'OPPFYLT'
WHERE utfall = 'IKKE_VURDERT'
  and vilkar_id IN (SELECT vv.id
                    from vr_vilkar vv
                             INNER JOIN rs_vilkars_resultat rv on vv.vilkar_resultat_id = rv.vilkarene_id
                             INNER JOIN vr_vilkar_periode vp on vv.id = vp.vilkar_id
                             INNER JOIN behandling b on rv.behandling_id = b.id
                             INNER JOIN fagsak f on b.fagsak_id = f.id
                    WHERE f.ytelse_type = 'OMP'
                      and rv.aktiv = true
                      and b.behandling_status = 'AVSLU'
                      and vv.vilkar_type = 'K9_VK_1');
