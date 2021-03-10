package no.nav.k9.sak.domene.risikoklassifisering.konsument;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

import static no.nav.k9.felles.feil.LogLevel.WARN;

public interface RisikoklassifiseringConsumerFeil extends DeklarerteFeil {

    RisikoklassifiseringConsumerFeil FACTORY = FeilFactory.create(RisikoklassifiseringConsumerFeil.class);

    @TekniskFeil(feilkode = "FP-65747", feilmelding = "Klarte ikke deserialisere for klasse %s", logLevel = WARN)
    Feil klarteIkkeDeserialisere(String className, Exception e);
}
