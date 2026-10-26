package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.felles.util.Tuple;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.inntektskontroll.KontrollertePerioderRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.uttalelse.UttalelseRecord;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Bestemmer hvilke metrikker som publiseres til BigQuery for én ytelse, og til hvilke tabeller.
 * <p>
 * Implementasjonene annoteres med {@link FagsakYtelseTypeRef} og bruker de felles, ytelsesparametriserte
 * spørringene i {@link BigQueryStatistikkRepository}. Ytelsesspesifikke metrikker og tabellnavn hører hjemme i
 * implementasjonen, slik at tabelldefinisjonene i record-klassene er uavhengige av ytelse.
 */
public interface YtelseStatistikk {

    /**
     * Ytelser det publiseres metrikker for. {@link FagsakYtelseType#OBSOLETE} (utgåtte fagsaker) og
     * {@link FagsakYtelseType#UDEFINERT} skal aldri med i statistikken.
     */
    List<FagsakYtelseType> STØTTEDE_YTELSER = List.of(FagsakYtelseType.UNGDOMSYTELSE, FagsakYtelseType.AKTIVITETSPENGER);

    List<Tuple<BigQueryTabell<?>, Collection<?>>> hentHyppigRapporterte(LocalDateTime sistKjørtTidspunkt);

    List<Tuple<BigQueryTabell<?>, Collection<?>>> hentDagligRapporterte();

    /**
     * Tabellen for uttalelser. Brukes også ved historisk populering ({@link PopulerMetrikkHistoriskeDataTask}).
     */
    BigQueryTabell<UttalelseRecord> uttalelseTabell();

    /**
     * Tabellen for kontrollerte inntektsperioder, som publiseres per vedtak ({@link KontrollerteInntektPerioderMetrikkPubliserer}).
     */
    BigQueryTabell<KontrollertePerioderRecord> kontrollertePerioderTabell();

    static boolean erStøttet(FagsakYtelseType ytelse) {
        return STØTTEDE_YTELSER.contains(ytelse);
    }

    static FagsakYtelseType validerStøttet(FagsakYtelseType ytelse) {
        if (!erStøttet(ytelse)) {
            throw new IllegalArgumentException("Ikke støttet ytelse for BigQuery-metrikker: " + ytelse);
        }
        return ytelse;
    }

    static <T extends BigQueryRecord> Tuple<BigQueryTabell<?>, Collection<?>> metrikk(BigQueryTabell<T> tabell, Collection<T> records) {
        return new Tuple<>(tabell, records);
    }

    static YtelseStatistikk finn(Instance<YtelseStatistikk> instanser, FagsakYtelseType ytelse) {
        validerStøttet(ytelse);
        return FagsakYtelseTypeRef.Lookup.find(instanser, ytelse)
            .orElseThrow(() -> new IllegalStateException("Mangler BigQuery-statistikk for ytelse " + ytelse));
    }
}
