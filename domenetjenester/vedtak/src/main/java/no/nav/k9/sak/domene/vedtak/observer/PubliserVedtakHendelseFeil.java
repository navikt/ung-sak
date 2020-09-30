package no.nav.k9.sak.domene.vedtak.observer;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface PubliserVedtakHendelseFeil extends DeklarerteFeil {
    PubliserVedtakHendelseFeil FEILFACTORY = FeilFactory.create(PubliserVedtakHendelseFeil.class); //$NON-NLS-1$

    @TekniskFeil(feilkode = "FP-190495", feilmelding = "Kunne ikke serialisere til json.", logLevel = LogLevel.WARN)
    Feil kanIkkeSerialisere(JsonProcessingException e);
}
