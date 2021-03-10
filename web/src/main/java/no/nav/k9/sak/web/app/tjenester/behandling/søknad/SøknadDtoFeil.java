package no.nav.k9.sak.web.app.tjenester.behandling.søknad;

import static no.nav.k9.felles.feil.LogLevel.ERROR;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface SøknadDtoFeil extends DeklarerteFeil {
    SøknadDtoFeil FACTORY = FeilFactory.create(SøknadDtoFeil.class);

    @TekniskFeil(feilkode = "FP-175810", feilmelding = "Ektefelle kan ikke være samme person som søker", logLevel = ERROR)
    Feil kanIkkeVæreSammePersonSomSøker();


}
