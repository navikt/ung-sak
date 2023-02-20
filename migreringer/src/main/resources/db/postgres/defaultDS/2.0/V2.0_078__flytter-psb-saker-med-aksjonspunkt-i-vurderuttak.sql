-- VURDER_UTTAK fjernes, er erstattet av VURDER_UTTAK_V2
-- flytter behandlinger som står på VURDER_UTTAK til neste steg i modellen

update aksjonspunkt
set behandling_steg_funnet = 'PRECONDITION_BERGRUNN',
    endret_tid             = current_timestamp at time zone 'UTC',
    endret_av              = 'migrering V2.0_078'
where id in (select a.id
             from aksjonspunkt a
                      join behandling b on b.id = a.behandling_id
                      join fagsak f on b.fagsak_id = f.id
             where a.behandling_steg_funnet = 'VURDER_UTTAK'
               and a.aksjonspunkt_status = 'OPPR'
               and ytelse_type = 'PSB');
