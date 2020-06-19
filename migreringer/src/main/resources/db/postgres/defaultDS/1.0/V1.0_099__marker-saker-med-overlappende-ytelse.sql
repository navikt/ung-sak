INSERT INTO PROSESS_TASK_TYPE (KODE,
                               NAVN,
                               FEIL_MAKS_FORSOEK,
                               FEIL_SEK_MELLOM_FORSOEK,
                               FEILHANDTERING_ALGORITME,
                               BESKRIVELSE)
VALUES ('iverksetteVedtak.vurderOverlappendeYtelser',
        'Vurder overlappende ytelser',
        2,
        30,
        'DEFAULT',
        'Vurder overlappende ytelser');

UPDATE aksjonspunkt
set frist_tid = current_date - interval '1 day'
WHERE aksjonspunkt_def = '9999'
  AND aksjonspunkt_status = 'OPPR';
