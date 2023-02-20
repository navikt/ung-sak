
-- VURDER_UTTAK fjernes, er erstattet av VURDER_UTTAK_V2
-- flytter behandlinger som står på VURDER_UTTAK til neste steg i modellen

update behandling_steg_tilstand
set behandling_steg = 'PRECONDITION_BERGRUNN',
    endret_tid = current_timestamp at time zone 'UTC',
    endret_av = 'migrering V2.0_077'
where id in (
    select bst.id from behandling_steg_tilstand bst
                           join behandling b on b.id = bst.behandling_id
                           join fagsak f on b.fagsak_id = f.id
    where
            bst.behandling_steg = 'VURDER_UTTAK'
      and bst.behandling_steg_status not in ('UTFØRT')
      and bst.aktiv
      and ytelse_type = 'PSB'
);
