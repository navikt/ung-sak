-- kjoerer denne etter andre tasks på samme fagsak. deadlock da fortsett behandling på senere behandling sperrer iverksetting (når iverksetting har feilet) og fortsett behandling ikke får kjørt (fordi iverketting har feilet mot oppdrag)

update fagsak_prosess_task set gruppe_sekvensnr=1603439188897 where id=4261353 and prosess_task_id=12190774;

update prosess_task set blokkert_av=null where id in (
12205465,
12205466,
12205469);
