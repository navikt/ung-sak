INSERT INTO gr_beregningsgrunnlag (id, BEHANDLING_ID, bg_grunnlag_id)
SELECT nextval('SEQ_GR_BEREGNINGSGRUNNLAG') as id, behandling_id, nextval('SEQ_BG_PERIODER')
FROM rs_vilkars_resultat
WHEre aktiv = true;

INSERT INTO BG_PERIODER (id)
SELECT bg_grunnlag_id
FROM gr_beregningsgrunnlag;

INSERT INTO BG_PERIODE (id, ekstern_referanse, skjaeringstidspunkt, bg_grunnlag_id)
SELECT nextval('SEQ_BG_PERIODE') as id,
       (SELECT uuid FROM behandling where id = rs.behandling_id),
       vp.fom,
       (SELECT bg_grunnlag_id from gr_beregningsgrunnlag gr where gr.behandling_id = rs.behandling_id)
FROM rs_vilkars_resultat rs
INNER JOIN vr_vilkar vv ON vv.vilkar_resultat_id = rs.id
INNER JOIN vr_vilkar_periode vp ON vv.id = vp.vilkar_id
WHERE rs.aktiv = true
AND vv.vilkar_type = 'FP_VK_41';
