package no.nav.k9.sak.web.app.tjenester.behandling.vilkår.aksjonspunkt;

import static no.nav.k9.felles.feil.LogLevel.WARN;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.FunksjonellFeil;

public interface OverstyringFeil extends DeklarerteFeil {
    OverstyringFeil FACTORY = FeilFactory.create(OverstyringFeil.class);

    @FunksjonellFeil(feilkode = "FP-093923", feilmelding = "Kan ikke overstyre vilkår. Det må være minst en aktivitet for at opptjeningsvilkåret skal kunne overstyres.",
        løsningsforslag = "Sett på vent til det er mulig og manuelt legge inn aktiviteter ved overstyring.", logLevel = WARN)
    Feil opptjeningPreconditionFailed();

}
