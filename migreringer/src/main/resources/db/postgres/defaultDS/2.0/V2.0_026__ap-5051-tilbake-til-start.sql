-- Spol behandlinger med 5051 tilbake til start, slik at ingen behandlinger lenger bruker dette obsolete aksjonspunktet
insert into prosess_task (id, task_type, task_gruppe, neste_kjoering_etter, task_parametere)
select nextval('seq_prosess_task'), 'behandlingskontroll.tilbakeTilStart',
       nextval('seq_prosess_task_gruppe'), null,
       'fagsakId=' || b.fagsak_id || '
                behandlingId=' || b.id
from behandling b
         inner join fagsak f on f.id=b.fagsak_id
         inner join aksjonspunkt a on a.behandling_id = b.id
where a.aksjonspunkt_def = '5051'
  and a.aksjonspunkt_status = 'OPPR'
;
