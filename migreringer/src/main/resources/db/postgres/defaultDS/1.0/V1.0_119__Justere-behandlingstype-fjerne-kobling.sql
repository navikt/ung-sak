ALTER TABLE behandling
    add column original_behandling_id bigint references behandling;

update behandling b
set original_behandling_id = (select DISTINCT original_behandling_id
                              from behandling_arsak ba
                              where ba.behandling_id = b.id);

update behandling
set original_behandling_id = null,
    behandling_type        = 'BT-002'
where uuid IN ('10a86dfd-59bc-4fc2-acf6-e6f2ceac8299', 'cc82d018-9b31-422d-9602-8cfd01ca502d',
               '9c8b3c8e-22a6-49ef-a428-9958d5ed49b7', 'c13a2999-5b1c-4a83-af2b-71b65bbbd916',
               '664f7751-31a5-44b0-81d7-da53d845a97f', '7e8479ca-7ab1-4463-84c4-03157cbcc2cb')
  and behandling_type = 'BT-004';
