package no.nav.k9.sak.domene.vedtak.observer;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface PubliserVedtakHendelseFeil extends DeklarerteFeil {
    PubliserVedtakHendelseFeil FEILFACTORY = FeilFactory.create(PubliserVedtakHendelseFeil.class); //$NON-NLS-1$

    @TekniskFeil(feilkode = "FP-190495", feilmelding = "Kunne ikke serialisere til json.", logLevel = LogLevel.WARN)
    Feil kanIkkeSerialisere(JsonProcessingException e);
}
