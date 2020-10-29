
-- rokkerer eksisterende task bakerst
update fagsak_prosess_task set gruppe_sekvensnr = 10*gruppe_sekvensnr
where behandling_id="1269308" and fagsak_id=1047857;

