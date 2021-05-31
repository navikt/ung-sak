-- saksnummer A9Si0, må sortere om tasks for å lagre oppgitt opptjening fra søknad korrekt

-- For blokkert fagsak_prosess_task: Sett lavere gruppe_sekvensnr enn blokkerende task (skal da kjøres først)
update fagsak_prosess_task set gruppe_sekvensnr = (1621634315576 - 1) where prosess_task_id = 16321043;
-- For tilhørende blokkert prosess_task: Fjern VETO/sett som KLAR
update prosess_task set status='KLAR', blokkert_av = null where id = 16321043 AND status in ('VETO');
