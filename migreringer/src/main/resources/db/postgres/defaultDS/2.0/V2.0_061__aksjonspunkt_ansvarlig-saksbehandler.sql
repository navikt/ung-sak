ALTER TABLE aksjonspunkt add column ansvarlig_saksbehandler VARCHAR(40);

INSERT INTO prosess_task_type (KODE,NAVN,FEIL_MAKS_FORSOEK,FEIL_SEK_MELLOM_FORSOEK,FEILHANDTERING_ALGORITME,BESKRIVELSE,CRON_EXPRESSION)
VALUES ('produksjonsstyring.publiserHendelse',
        'Publiser eventer på kafka for oppgavestyring.',
        3,
        60,
        'DEFAULT',
        'Publiser eventer på kafka (Aksjonspunkter, innkomne dokumenter, Vedtak) slik at k9-los kan fordele oppgaver for videre behandling.',
        null
        );
