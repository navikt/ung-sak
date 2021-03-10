package no.nav.k9.sak.kompletthet;

import static no.nav.k9.felles.feil.LogLevel.WARN;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface KompletthetFeil extends DeklarerteFeil {

    KompletthetFeil FACTORY = FeilFactory.create(KompletthetFeil.class);

    @TekniskFeil(feilkode = "FP-912911", feilmelding = "Mer enn en implementasjon funnet av Kompletthetsjekker for fagsakYtelseType=%s og behandlingType=%s", logLevel = WARN)
    Feil flereImplementasjonerAvKompletthetsjekker(String fagsakYtelseType, String behandlingType);

    @TekniskFeil(feilkode = "FP-912910", feilmelding = "Fant ingen implementasjon av Kompletthetsjekker for fagsakYtelseType=%s og behandlingType=%s", logLevel = WARN)
    Feil ingenImplementasjonerAvKompletthetssjekker(String fagsakYtelseType, String behandlingType);

}
