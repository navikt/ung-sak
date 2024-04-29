UPDATE vr_vilkar_periode
SET utfall = 'OPPFYLT',
    regel_evaluering = 'Utfall kopiert fra behandling 1611637 (TSF-3211)'
WHERE id IN (SELECT vp.id
             from vr_vilkar vv
                      INNER JOIN rs_vilkars_resultat rv on vv.vilkar_resultat_id = rv.vilkarene_id
                      INNER JOIN vr_vilkar_periode vp on vv.id = vp.vilkar_id
             WHERE rv.aktiv = true
               and vp.utfall = 'IKKE_VURDERT'
               and rv.behandling_id = 1611655
               and vv.vilkar_type = 'FP_VK_23');
