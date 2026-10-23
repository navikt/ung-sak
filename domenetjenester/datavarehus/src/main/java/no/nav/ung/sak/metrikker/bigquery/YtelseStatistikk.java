package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.felles.util.Tuple;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import no.nav.ung.sak.metrikker.bigquery.tabeller.YtelsesTabell;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Bestemmer hvilke metrikker som publiseres til BigQuery for én ytelse, og til hvilke tabeller.
 * <p>
 * Implementasjonene annoteres med {@link FagsakYtelseTypeRef} og bruker de felles, ytelsesparametriserte
 * spørringene i {@link BigQueryStatistikkRepository}. Ytelsesspesifikke metrikker hører hjemme i implementasjonen.
 */
public interface YtelseStatistikk {

    List<Tuple<BigQueryTabell<?>, Collection<?>>> hentHyppigRapporterte(LocalDateTime sistKjørtTidspunkt);

    List<Tuple<BigQueryTabell<?>, Collection<?>>> hentDagligRapporterte();

    static <T extends BigQueryRecord> Tuple<BigQueryTabell<?>, Collection<?>> metrikk(BigQueryTabell<T> tabell, Collection<T> records) {
        return new Tuple<>(tabell, records);
    }

    static YtelseStatistikk finn(Instance<YtelseStatistikk> instanser, FagsakYtelseType ytelse) {
        YtelsesTabell.validerStøttet(ytelse);
        return FagsakYtelseTypeRef.Lookup.find(instanser, ytelse)
            .orElseThrow(() -> new IllegalStateException("Mangler BigQuery-statistikk for ytelse " + ytelse));
    }
}
