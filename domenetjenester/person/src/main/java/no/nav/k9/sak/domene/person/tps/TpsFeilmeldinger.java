package no.nav.k9.sak.domene.person.tps;

import static no.nav.k9.felles.feil.LogLevel.WARN;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface TpsFeilmeldinger extends DeklarerteFeil {

    TpsFeilmeldinger FACTORY = FeilFactory.create(TpsFeilmeldinger.class);

    @TekniskFeil(feilkode = "FP-164686", feilmelding = "Person er ikke Bruker, kan ikke hente ut brukerinformasjon", logLevel = LogLevel.WARN)
    Feil ukjentBrukerType();

    @TekniskFeil(feilkode = "FP-181235", feilmelding = "Fant ikke aktørId i TPS", logLevel = WARN)
    Feil fantIkkePersonForAktørId();

}
