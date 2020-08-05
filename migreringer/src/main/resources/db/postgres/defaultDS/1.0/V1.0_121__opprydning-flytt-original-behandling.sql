update behandling b
set original_behandling_id = (select DISTINCT original_behandling_id
                              from behandling_arsak ba
                              where ba.behandling_id = b.id)
WHERE behandling_type = 'BT-004'
  and original_behandling_id = null;
