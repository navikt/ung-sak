package no.nav.k9.sak.domene.arbeidsforhold.person;

import static no.nav.k9.felles.feil.LogLevel.WARN;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

interface PersonIdentFeilmeldinger extends DeklarerteFeil {

    PersonIdentFeilmeldinger FACTORY = FeilFactory.create(PersonIdentFeilmeldinger.class);

    @TekniskFeil(feilkode = "FP-181237", feilmelding = "Fant ikke aktørId i TPS", logLevel = WARN)
    Feil fantIkkePersonForAktørId();
}
