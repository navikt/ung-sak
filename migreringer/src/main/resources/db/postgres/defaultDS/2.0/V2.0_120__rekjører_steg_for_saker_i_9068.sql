insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id || '
                startSteg=KOMPLETT_FOR_BEREGNING'
from behandling b
         inner join aksjonspunkt a on b.id = a.behandling_id
where a.aksjonspunkt_def = '9068'
  and a.aksjonspunkt_status = 'OPPR'
  and a.opprettet_tid >= to_date('2023-01-27', 'YYYY-MM-DD') and a.opprettet_tid < to_date('2023-02-01', 'YYYY-MM-DD')
  and (a.endret_tid is null OR a.endret_tid < to_date('2023-02-01', 'YYYY-MM-DD'))
  and b.behandling_status='UTRED';

