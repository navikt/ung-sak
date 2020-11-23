package no.nav.k9.sak.domene.abakus;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public interface AbakusInntektArbeidYtelseTjenesteFeil extends DeklarerteFeil {
    AbakusInntektArbeidYtelseTjenesteFeil FEIL = FeilFactory.create(AbakusInntektArbeidYtelseTjenesteFeil.class);

    @TekniskFeil(feilkode = "FP-118669", feilmelding = "Feil ved kall til Abakus: %s", logLevel = LogLevel.WARN)
    Feil feilVedKallTilAbakus(String feilmelding, Throwable t);

}