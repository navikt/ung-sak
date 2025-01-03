package no.nav.ung.sak.historikk;

import static no.nav.k9.felles.feil.LogLevel.ERROR;

import java.util.List;
import java.util.Set;

import no.nav.k9.felles.feil.Feil;
import no.nav.k9.felles.feil.FeilFactory;
import no.nav.k9.felles.feil.deklarasjon.DeklarerteFeil;
import no.nav.k9.felles.feil.deklarasjon.TekniskFeil;

interface HistorikkInnsalgFeil extends DeklarerteFeil {
    HistorikkInnsalgFeil FACTORY = FeilFactory.create(HistorikkInnsalgFeil.class);

    @TekniskFeil(feilkode = "FP-876694", feilmelding = "For type %s, mangler felter %s for historikkinnslag.", logLevel = ERROR)
    Feil manglerFeltForHistorikkInnslag(String type, List<String> manglendeFelt);

    @TekniskFeil(feilkode = "FP-876693", feilmelding = "For type %s, forventer minst et felt av type %s. Eksisterende felttyper er %s", logLevel = ERROR)
    Feil manglerMinstEtFeltForHistorikkinnslag(String type, List<String> manglendeFelt, Set<String> eksisterendeKoder);

    @TekniskFeil(feilkode = "FP-876692", feilmelding = "Ukjent historikkinnslagstype: %s", logLevel = ERROR)
    Feil ukjentHistorikkinnslagType(String kode);
}
