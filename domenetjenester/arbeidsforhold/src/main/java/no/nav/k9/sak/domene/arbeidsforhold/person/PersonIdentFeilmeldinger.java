package no.nav.k9.sak.domene.arbeidsforhold.person;

import static no.nav.vedtak.feil.LogLevel.WARN;

import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

interface PersonIdentFeilmeldinger extends DeklarerteFeil {

    PersonIdentFeilmeldinger FACTORY = FeilFactory.create(PersonIdentFeilmeldinger.class);

    @TekniskFeil(feilkode = "FP-181237", feilmelding = "Fant ikke aktørId i TPS", logLevel = WARN)
    Feil fantIkkePersonForAktørId();
}
