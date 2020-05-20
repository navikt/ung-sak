UPDATE vr_vilkar_periode
set fom = '2020-03-01'
where vilkar_id IN (SELECT vp.vilkar_id
                    FROM rs_vilkars_resultat rs
                             inner join behandling b ON rs.behandling_id = b.id
                             inner join fagsak f on b.fagsak_id = f.id
                             INNER JOIN vr_vilkar vv ON vv.vilkar_resultat_id = rs.vilkarene_id
                             INNER JOIN vr_vilkar_periode vp ON vv.id = vp.vilkar_id
                    WHERE rs.aktiv = true
                      AND vv.vilkar_type = 'FP_VK_41'
                      AND f.ytelse_type = 'FRISINN'
                      AND vp.fom != '2020-03-01');

update bg_periode
set skjaeringstidspunkt='2020-03-01'
where bg_grunnlag_id in (SELECT vv.bg_grunnlag_id
                         FROM gr_beregningsgrunnlag rs
                                  inner join behandling b ON rs.behandling_id = b.id
                                  inner join fagsak f on b.fagsak_id = f.id
                                  INNER JOIN bg_periode vv ON vv.bg_grunnlag_id = rs.bg_grunnlag_id
                         WHERE rs.aktiv = true
                           AND f.ytelse_type = 'FRISINN'
                           AND vv.skjaeringstidspunkt != '2020-03-01');
