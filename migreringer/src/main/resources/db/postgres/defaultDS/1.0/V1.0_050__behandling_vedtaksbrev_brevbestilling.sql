update behandling_vedtaksbrev bvb
set brevbestilling_id = bb.id
from brevbestilling bb
where bvb.behandling_id = bb.behandling_id
  and bb.vedtaksbrev = true
  and bb.aktiv = true
  and bvb.resultat_type = 'BESTILT'
  and bvb.brevbestilling_id is null
  and (
          select count(*)
          from brevbestilling bb2
          where bb2.behandling_id = bvb.behandling_id
      ) = 1;
