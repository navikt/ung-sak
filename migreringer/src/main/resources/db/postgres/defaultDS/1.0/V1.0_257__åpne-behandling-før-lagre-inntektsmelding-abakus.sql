-- saksnummer 9SXWi, må sortere om tasks for å kunne få inn inntektsmeldinger
update fagsak_prosess_task set gruppe_sekvensnr = (1614768811486 - 1) where id=5458320;
update prosess_task set status='KLAR', blokkert_av=null where id=15637815;