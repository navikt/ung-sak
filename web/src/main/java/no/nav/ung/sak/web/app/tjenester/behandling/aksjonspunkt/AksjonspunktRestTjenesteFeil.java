package no.nav.ung.sak.web.app.tjenester.behandling.aksjonspunkt;

import static no.nav.k9.felles.feil.LogLevel.WARN;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.FunksjonellFeil;

public interface AksjonspunktRestTjenesteFeil extends DeklarerteFeil {
    AksjonspunktRestTjenesteFeil FACTORY = FeilFactory.create(AksjonspunktRestTjenesteFeil.class);

    @FunksjonellFeil(feilkode = "K9-760743", feilmelding = "Det kan ikke akseptere endringer siden totrinnsbehandling er startet og behandlingen med behandlingId: %s er hos beslutter", løsningsforslag = "Avklare med beslutter", logLevel = WARN)
    Feil totrinnsbehandlingErStartet(String behandlingId);

    @FunksjonellFeil(feilkode = "K9-932741", feilmelding = "Kan ikke løse aksjonspunkter når behandlingen med behandlingId: %s er satt på vent", løsningsforslag = "Ta behandlingen av vent før aksjonspunktene løses", logLevel = WARN)
    Feil kanIkkeLøseAksjonspunkterNårBehandlingPåVent(String behandlingId);
}
