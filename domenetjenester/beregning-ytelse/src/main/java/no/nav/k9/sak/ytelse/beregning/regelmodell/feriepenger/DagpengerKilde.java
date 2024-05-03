package no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger;

/**
 * Kilde til dagpenger, enten direkte utbetalt fra meldekort eller i form av ytelse beregnet etter ftrl. §8-49
 */
public enum DagpengerKilde {
    MELDEKORT,
    PLEIEPENGER_SYKT_BARN,
    PLEIEPENGER_NÆRSTÅENDE,
    OPPLÆRINGSPENGER,
    SYKEPENGER,
    FORELDREPENGER
}
