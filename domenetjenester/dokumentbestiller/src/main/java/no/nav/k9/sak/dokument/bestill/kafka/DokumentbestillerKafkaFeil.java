package no.nav.k9.sak.dokument.bestill.kafka;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface DokumentbestillerKafkaFeil extends DeklarerteFeil {

    DokumentbestillerKafkaFeil FACTORY = FeilFactory.create(DokumentbestillerKafkaFeil.class);

    @TekniskFeil(feilkode = "FP-533280", feilmelding = "Klarte ikke utlede ytelsetype: %s. Kan ikke bestille dokument", logLevel = LogLevel.ERROR)
    Feil fantIkkeYtelseType(String ytelseType);

}
