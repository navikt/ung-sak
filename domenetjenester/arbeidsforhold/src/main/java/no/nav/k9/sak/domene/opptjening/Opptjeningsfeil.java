package no.nav.k9.sak.domene.opptjening;

import static no.nav.k9.felles.feil.LogLevel.WARN;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.FunksjonellFeil;

public interface Opptjeningsfeil extends DeklarerteFeil {
    Opptjeningsfeil FACTORY = FeilFactory.create(Opptjeningsfeil.class);

    @FunksjonellFeil(feilkode = "FP-093922", feilmelding = "Kan ikke sette opptjeningsvilkåret til oppfylt. Det må være minst en aktivitet for at opptjeningsvilkåret skal kunne settets til oppfylt.",
        løsningsforslag = "Sett på vent til det er mulig og manuelt legge inn aktiviteter ved overstyring.", logLevel = WARN)
    Feil opptjeningPreconditionFailed();
}

