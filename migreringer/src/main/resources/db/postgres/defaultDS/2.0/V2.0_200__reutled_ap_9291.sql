-- Lar saker som står i aksjonspunkt 9291 kjøre dette steget på nytt for å reutlede aksjonspunkt
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id || '
                startSteg=VURDER_STARTDATO_UTTAKSREGLER'
from fagsak f

                                                                                     inner join behandling b on f.id = b.fagsak_id
                                                                                     inner join aksjonspunkt a on b.id = a.behandling_id
where a.aksjonspunkt_def = '9291'
  and a.aksjonspunkt_status = 'OPPR'
  and a.opprettet_tid < '2024-10-22 16:00:00.000';
