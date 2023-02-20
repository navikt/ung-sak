insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id || '
                startSteg=VURDER_OMSORG_FOR'
from behandling b
         inner join aksjonspunkt a on b.id = a.behandling_id
         inner join fagsak f on f.id = b.fagsak_id
where a.aksjonspunkt_def = '9002' and a.aksjonspunkt_status = 'OPPR' and b.behandling_status='UTRED' and f.ytelse_type = 'OMP_KS';
