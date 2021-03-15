package no.nav.k9.sak.ytelse.beregning;

import static no.nav.k9.felles.feil.LogLevel.ERROR;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

public interface FinnEndringsdatoFeil extends DeklarerteFeil {

    FinnEndringsdatoFeil FACTORY = FeilFactory.create(FinnEndringsdatoFeil.class);

    @TekniskFeil(feilkode = "FP-655544", feilmelding = "Behandlingen med id %s er ikke en revurdering", logLevel = ERROR)
    Feil behandlingErIkkeEnRevurdering(Long behandlingId);

    @TekniskFeil(feilkode = "FP-655545", feilmelding = "Fant ikke en original behandling for revurdering med id %s", logLevel = ERROR)
    Feil manglendeOriginalBehandling(Long behandlingId);

    @TekniskFeil(feilkode = "FP-655542", feilmelding = "Fant ikke beregningsresultatperiode for beregningsresultat med id %s", logLevel = ERROR)
    Feil manglendeBeregningsresultatPeriode(Long beregningsresultatId);

    @TekniskFeil(feilkode = "FP-655546", feilmelding = "Fant flere korresponderende andeler for andel med id %s", logLevel = ERROR)
    Feil fantFlereKorresponderendeAndelerFeil(Long beregningsresultatAndelId);

}
