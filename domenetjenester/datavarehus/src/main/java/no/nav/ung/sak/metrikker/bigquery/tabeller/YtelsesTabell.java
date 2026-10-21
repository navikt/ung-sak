package no.nav.ung.sak.metrikker.bigquery.tabeller;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.metrikker.bigquery.BigQueryRecord;

import java.util.List;
import java.util.Objects;

/**
 * Samme metrikk publisert til én tabell per ytelse, slik at tilgang kan styres per ytelse på Datamarkedsplassen.
 * Tabellene har samme skjema og mapper.
 */
public record YtelsesTabell<T extends BigQueryRecord>(
    BigQueryTabell<T> ungdomsytelse,
    BigQueryTabell<T> aktivitetspenger
) {

    /**
     * Ytelser det publiseres metrikker for. {@link FagsakYtelseType#OBSOLETE} (utgåtte fagsaker) og
     * {@link FagsakYtelseType#UDEFINERT} skal aldri med i statistikken.
     */
    public static final List<FagsakYtelseType> STØTTEDE_YTELSER = List.of(FagsakYtelseType.UNGDOMSYTELSE, FagsakYtelseType.AKTIVITETSPENGER);

    public static boolean erStøttet(FagsakYtelseType ytelse) {
        return STØTTEDE_YTELSER.contains(ytelse);
    }

    public static FagsakYtelseType validerStøttet(FagsakYtelseType ytelse) {
        if (!erStøttet(ytelse)) {
            throw new IllegalArgumentException("Ikke støttet ytelse for BigQuery-metrikker: " + ytelse);
        }
        return ytelse;
    }

    public YtelsesTabell {
        Objects.requireNonNull(ungdomsytelse, "ungdomsytelse");
        Objects.requireNonNull(aktivitetspenger, "aktivitetspenger");
    }

    public static <T extends BigQueryRecord> YtelsesTabell<T> medAktivitetspengerTabell(BigQueryTabell<T> ungdomsytelse, String aktivitetspengerTabellnavn) {
        return new YtelsesTabell<>(ungdomsytelse, ungdomsytelse.medTabellnavn(aktivitetspengerTabellnavn));
    }

    public BigQueryTabell<T> forYtelse(FagsakYtelseType ytelse) {
        return switch (validerStøttet(ytelse)) {
            case UNGDOMSYTELSE -> ungdomsytelse;
            case AKTIVITETSPENGER -> aktivitetspenger;
            default -> throw new IllegalStateException("Mangler tabell for ytelse " + ytelse);
        };
    }

    public List<BigQueryTabell<T>> alle() {
        return List.of(ungdomsytelse, aktivitetspenger);
    }
}
