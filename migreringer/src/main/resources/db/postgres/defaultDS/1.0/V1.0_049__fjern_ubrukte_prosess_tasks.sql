

delete from fagsak_prosess_task where prosess_task_id in (
	select id from prosess_task where task_type in (
		'oppgavebehandling.opprettOppgaveBehandleSak',
		'oppgavebehandling.opprettOppgaveRegistrerSøknad',
		'oppgavebehandling.opprettOppgaveGodkjennVedtak',
		'behandlingsstotte.sendForlengelsesbrev',
		'oppgavebehandling.opprettOppgaveSakSendtTilbake',
		'hendelser.klargjoering',
		'hendelser.håndterHendelsePåFagsak',
		'iverksetteVedtak.sendTilkjentYtelse',
		'migrer.sendTilkjentYtelse',
		'mottak.publiserPersistertDokument'
	)
);

delete from prosess_task where task_type in (
	'oppgavebehandling.opprettOppgaveBehandleSak',
	'oppgavebehandling.opprettOppgaveRegistrerSøknad',
	'oppgavebehandling.opprettOppgaveGodkjennVedtak',
	'behandlingsstotte.sendForlengelsesbrev',
	'oppgavebehandling.opprettOppgaveSakSendtTilbake',
	'hendelser.klargjoering',
	'hendelser.håndterHendelsePåFagsak',
	'iverksetteVedtak.sendTilkjentYtelse',
	'migrer.sendTilkjentYtelse',
	'mottak.publiserPersistertDokument'
);

delete from prosess_task_type where kode in (
	'oppgavebehandling.opprettOppgaveBehandleSak',
	'oppgavebehandling.opprettOppgaveRegistrerSøknad',
	'oppgavebehandling.opprettOppgaveGodkjennVedtak',
	'behandlingsstotte.sendForlengelsesbrev',
	'oppgavebehandling.opprettOppgaveSakSendtTilbake',
	'hendelser.klargjoering',
	'hendelser.håndterHendelsePåFagsak',
	'iverksetteVedtak.sendTilkjentYtelse',
	'migrer.sendTilkjentYtelse',
	'mottak.publiserPersistertDokument'
);