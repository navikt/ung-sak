package no.nav.k9.sak.dokument.bestill;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface BrevFeil extends DeklarerteFeil {
    BrevFeil FACTORY = FeilFactory.create(BrevFeil.class);

    @TekniskFeil(feilkode = "FP-666915", feilmelding = "Ingen brevmal konfigurert for denne type behandlingen %d.", logLevel = LogLevel.ERROR)
    Feil ingenBrevmalKonfigurert(Long behandlingId);
}
