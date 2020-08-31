update behandling
set original_behandling_id = null,
    behandling_type        = 'BT-002'
where id = 1097766
  and behandling_type = 'BT-004';
