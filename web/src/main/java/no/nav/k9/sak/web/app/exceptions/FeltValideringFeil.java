package no.nav.k9.sak.web.app.exceptions;


import java.util.List;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.LogLevel;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.FunksjonellFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

interface FeltValideringFeil extends DeklarerteFeil{

    FeltValideringFeil FACTORY = FeilFactory.create(FeltValideringFeil.class);

    @FunksjonellFeil(feilkode = "FP-328673",
        feilmelding = "Det oppstod en valideringsfeil på felt %s. Vennligst kontroller at alle feltverdier er korrekte.",
        løsningsforslag = "Kontroller at alle feltverdier er korrekte" ,logLevel = LogLevel.WARN)
    Feil feltverdiKanIkkeValideres(List<String> feltnavn);

    @TekniskFeil(feilkode = "FP-322345",
        feilmelding = "Det oppstod en serverfeil: Validering er feilkonfigurert." ,logLevel = LogLevel.ERROR)
    Feil feilIOppsettAvFeltvalidering();
}
