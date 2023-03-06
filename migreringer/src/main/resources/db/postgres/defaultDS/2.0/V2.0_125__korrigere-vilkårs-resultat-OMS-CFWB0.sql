UPDATE vr_vilkar_periode periodeTilOppdatering
SET utfall           = sub.utfall,
    regel_evaluering = sub.regel_evaluering FROM (SELECT vp.utfall, vp.regel_evaluering, vp.fom
                                                  from vr_vilkar vv
                                                           INNER JOIN rs_vilkars_resultat rv on vv.vilkar_resultat_id = rv.vilkarene_id
                                                           INNER JOIN vr_vilkar_periode vp on vv.id = vp.vilkar_id
                                                  WHERE rv.aktiv = true
                                                    and rv.behandling_id = 1611637
                                                    and vv.vilkar_type = 'FP_VK_23') as sub
WHERE periodeTilOppdatering.fom = sub.fom
  and periodeTilOppdatering.utfall = 'IKKE_VURDERT'
  and periodeTilOppdatering.id IN (SELECT vp.id
    from vr_vilkar vv
    INNER JOIN rs_vilkars_resultat rv on vv.vilkar_resultat_id = rv.vilkarene_id
    INNER JOIN vr_vilkar_periode vp on vv.id = vp.vilkar_id
    WHERE rv.aktiv = true
  and rv.behandling_id = 1611655
  and vv.vilkar_type = 'FP_VK_23');
