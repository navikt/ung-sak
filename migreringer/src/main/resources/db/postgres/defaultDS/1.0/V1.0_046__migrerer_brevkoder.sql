UPDATE fordel_journalpost_mottatt
SET brevkode = CASE brevkode
                   WHEN 'UNG Søknad' THEN 'NAV 76-13.92'
                   WHEN 'UNG Inntektrapportering' THEN 'NAV 76-13.93'
                   WHEN 'UNG Oppgavebekreftelse' THEN 'NAV 76-13.94'
                   ELSE brevkode
    END
WHERE brevkode IN (
                   'UNG Søknad',
                   'UNG Inntektrapportering',
                   'UNG Oppgavebekreftelse'
    );

UPDATE fordel_mottatt_melding
SET brevkode = CASE brevkode
                   WHEN 'UNG Søknad' THEN 'NAV 76-13.92'
                   WHEN 'UNG Inntektrapportering' THEN 'NAV 76-13.93'
                   WHEN 'UNG Oppgavebekreftelse' THEN 'NAV 76-13.94'
                   ELSE brevkode
    END
WHERE brevkode IN (
                   'UNG Søknad',
                   'UNG Inntektrapportering',
                   'UNG Oppgavebekreftelse'
    );

UPDATE fordel_journalpost_innsending
SET brevkode = CASE brevkode
                   WHEN 'UNG Søknad' THEN 'NAV 76-13.92'
                   WHEN 'UNG Inntektrapportering' THEN 'NAV 76-13.93'
                   WHEN 'UNG Oppgavebekreftelse' THEN 'NAV 76-13.94'
                   ELSE brevkode
    END
WHERE brevkode IN (
                   'UNG Søknad',
                   'UNG Inntektrapportering',
                   'UNG Oppgavebekreftelse'
    );
