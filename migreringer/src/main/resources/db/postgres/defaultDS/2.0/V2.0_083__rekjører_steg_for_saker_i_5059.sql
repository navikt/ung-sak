insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id || '
                startSteg=VURDER_REF_BERGRUNN'
from behandling b
         inner join aksjonspunkt a on b.id = a.behandling_id
where a.aksjonspunkt_def = '5059' and a.aksjonspunkt_status = 'OPPR' and a.opprettet_tid < '2022-04-20 12:55:00.000' and b.behandling_status='UTRED';

