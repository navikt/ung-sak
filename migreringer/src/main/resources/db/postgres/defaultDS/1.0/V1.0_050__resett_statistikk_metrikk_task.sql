update prosess_task_type set FEIL_MAKS_FORSOEK=240
where kode='sensu.metrikk.task';

update prosess_task set neste_kjoering_etter=null ,
		status='KLAR',
		siste_kjoering_feil_kode=null,
		siste_kjoering_feil_tekst=null,
		feilede_forsoek=0
		where task_type='sensu.metrikk.task';