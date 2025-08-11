-- dette skriptet funker kun for vedtaksbrev som har kun en bestilling. For flere må de patches hver for seg.
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
          and bb2.vedtaksbrev = true
          and bb2.aktiv = true
      ) = 1;

update brevbestilling bb
set mottaker_id = f.bruker_aktoer_id,
    mottaker_id_type = 'AKTØRID'
from fagsak f
where bb.fagsak_id = f.id
  and bb.mottaker_id is null
  and bb.mottaker_id_type is null
  and bb.aktiv = true;
