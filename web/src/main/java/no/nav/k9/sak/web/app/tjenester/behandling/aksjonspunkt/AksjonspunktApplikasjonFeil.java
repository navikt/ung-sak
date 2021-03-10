package no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt;

import static no.nav.k9.felles.feil.LogLevel.WARN;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface AksjonspunktApplikasjonFeil extends DeklarerteFeil {
    AksjonspunktApplikasjonFeil FACTORY = FeilFactory.create(AksjonspunktApplikasjonFeil.class);

    @TekniskFeil(feilkode = "FP-770743",
        feilmelding = "Finner ikke håndtering for aksjonspunkt med kode: %s", logLevel = WARN)
    Feil kanIkkeFinneAksjonspunktUtleder(String aksjonspunktKode);

    @TekniskFeil(feilkode = "FP-475766",
        feilmelding = "Finner ikke overstyringshåndterer for DTO: %s", logLevel = WARN)
    Feil kanIkkeFinneOverstyringshåndterer(String dtoNavn);

    @TekniskFeil(feilkode = "FP-605445",
        feilmelding = "Kan ikke aktivere aksjonspunkt med kode: %s", logLevel = LogLevel.ERROR)
    Feil kanIkkeAktivereAksjonspunkt(String aksjonspunktKode);
}
