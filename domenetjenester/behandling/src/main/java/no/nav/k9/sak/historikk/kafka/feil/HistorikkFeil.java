package no.nav.k9.sak.historikk.kafka.feil;

import static no.nav.k9.felles.feil.LogLevel.WARN;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface HistorikkFeil extends DeklarerteFeil {

    HistorikkFeil FACTORY = FeilFactory.create(HistorikkFeil.class);

    @TekniskFeil(feilkode = "FP-296632", feilmelding = "Klarte ikke deserialisere for klasse %s", logLevel = WARN)
    Feil klarteIkkeDeserialisere(String className, Throwable t);

    @TekniskFeil(feilkode = "FP-164754", feilmelding = "Mottok ugyldig journalpost ID %s", logLevel = WARN)
    Feil ugyldigJournalpost(String journalpost);
}
