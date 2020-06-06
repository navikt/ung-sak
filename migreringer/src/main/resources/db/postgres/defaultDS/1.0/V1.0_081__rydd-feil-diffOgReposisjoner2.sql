delete from fagsak_prosess_task where prosess_task_id in (1876028, 1881428) and fagsak_id IN (1019881, 1018903);
delete from fagsak_prosess_task where prosess_task_id in (select id from prosess_task WHERE TASK_TYPE='behandlingskontroll.fortsettBehandling' AND BLOKKERT_AV IN (1876028, 1881428) AND status IN ('KJOERT', 'FERDIG'));
