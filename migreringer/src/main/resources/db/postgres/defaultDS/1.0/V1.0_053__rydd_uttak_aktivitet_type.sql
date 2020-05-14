Insert into PROSESS_TASK_TYPE (KODE,NAVN,FEIL_MAKS_FORSOEK,FEIL_SEK_MELLOM_FORSOEK,FEILHANDTERING_ALGORITME,BESKRIVELSE,CRON_EXPRESSION)
values ('iverksetteVedtak.holdIgjenIverksettelse','Holder igjen iverksettelse av vedtak',3,30,'DEFAULT','Holder igjen iverksettelse av vedtak', '0 */5 * * * *');
