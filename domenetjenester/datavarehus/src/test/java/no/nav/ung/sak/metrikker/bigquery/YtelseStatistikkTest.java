package no.nav.ung.sak.metrikker.bigquery;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.util.Tuple;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.metrikker.bigquery.tabeller.BigQueryTabell;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.FieldSource;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class YtelseStatistikkTest {

    /**
     * Tabellene i BigQuery per ytelse. Metabase leser fra disse, så et endret navn gir en ny, tom tabell
     * og ødelegger eksisterende spørringer. Endres kun bevisst.
     */
    private static final Map<FagsakYtelseType, Set<String>> FORVENTEDE_TABELLNAVN = Map.of(
        FagsakYtelseType.UNGDOMSYTELSE, Set.of(
            "fagsak_status_v2",
            "behandling_status",
            "aksjonspunkter_status",
            "sats_statistikk",
            "behandlingsresultat_statistikk_v2",
            "etterlysning",
            "uttalelse",
            "behandling_årsak",
            "alder_og_kjønn",
            "dager_i_programmet_løpende",
            "kontrollerte_inntekt_perioder_v2"
        ),
        FagsakYtelseType.AKTIVITETSPENGER, Set.of(
            "aktivitetspenger_fagsak_status",
            "aktivitetspenger_behandling_status",
            "aktivitetspenger_aksjonspunkter_status",
            "aktivitetspenger_sats_statistikk",
            "aktivitetspenger_behandlingsresultat_statistikk",
            "aktivitetspenger_etterlysning",
            "aktivitetspenger_uttalelse",
            "aktivitetspenger_behandling_årsak",
            "aktivitetspenger_alder_og_kjønn",
            "aktivitetspenger_soknad_mottatt",
            "aktivitetspenger_kontrollerte_inntekt_perioder"
        )
    );

    @Inject
    @Any
    private Instance<YtelseStatistikk> ytelseStatistikk;

    /**
     * {@link JpaExtension} starter transaksjonen kun for {@code @Test} og {@code @BeforeEach}, ikke for
     * {@code @ParameterizedTest}. Uten transaksjon frigjøres ikke databasetilkoblingen etter testen,
     * og tilkoblingspoolen går tom for de neste testene.
     */
    @BeforeEach
    void startTransaksjon() {
        // transaksjonen startes av JpaExtension før denne metoden kjøres
    }

    @Test
    void skal_finne_egen_statistikk_for_hver_støttede_ytelse() {
        assertThat(YtelseStatistikk.finn(ytelseStatistikk, FagsakYtelseType.UNGDOMSYTELSE)).isInstanceOf(UngdomsprogramytelseStatistikk.class);
        assertThat(YtelseStatistikk.finn(ytelseStatistikk, FagsakYtelseType.AKTIVITETSPENGER)).isInstanceOf(AktivitetspengerStatistikk.class);
    }

    /**
     * Kjører alle spørringene mot Flyway-skjemaet, slik at en migrering som endrer en tabell eller kolonne
     * som statistikken leser fra, feiler her i stedet for i tasken som kjører hver time i produksjon.
     */
    @ParameterizedTest
    @FieldSource("no.nav.ung.sak.metrikker.bigquery.YtelseStatistikk#STØTTEDE_YTELSER")
    void skal_kunne_kjøre_alle_spørringer_mot_databaseskjemaet(FagsakYtelseType ytelse) {
        YtelseStatistikk statistikk = YtelseStatistikk.finn(ytelseStatistikk, ytelse);

        assertThatCode(() -> {
            statistikk.hentHyppigRapporterte(LocalDateTime.now().minusHours(1));
            statistikk.hentDagligRapporterte();
        }).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @FieldSource("no.nav.ung.sak.metrikker.bigquery.YtelseStatistikk#STØTTEDE_YTELSER")
    void skal_publisere_til_forventede_tabeller(FagsakYtelseType ytelse) {
        assertThat(FORVENTEDE_TABELLNAVN).containsKey(ytelse);

        Set<String> tabellnavn = alleTabeller(ytelse).stream().map(BigQueryTabell::getTabellnavn).collect(Collectors.toSet());

        assertThat(tabellnavn).containsExactlyInAnyOrderElementsOf(FORVENTEDE_TABELLNAVN.get(ytelse));
    }

    @ParameterizedTest
    @FieldSource("no.nav.ung.sak.metrikker.bigquery.YtelseStatistikk#STØTTEDE_YTELSER")
    void skal_populere_historiske_uttalelser_til_samme_tabell_som_løpende_publisering(FagsakYtelseType ytelse) {
        YtelseStatistikk statistikk = YtelseStatistikk.finn(ytelseStatistikk, ytelse);

        assertThat(hyppigRapporterteTabeller(statistikk)).contains(statistikk.uttalelseTabell());
    }

    @Test
    void skal_ha_unike_tabellnavn_og_samme_skjema_for_samme_metrikk_på_tvers_av_ytelser() {
        List<BigQueryTabell<?>> tabeller = YtelseStatistikk.STØTTEDE_YTELSER.stream()
            .flatMap(ytelse -> alleTabeller(ytelse).stream())
            .toList();

        assertThat(tabeller).extracting(BigQueryTabell::getTabellnavn).doesNotHaveDuplicates();

        Map<Class<?>, List<BigQueryTabell<?>>> tabellerPerRecord = tabeller.stream()
            .collect(Collectors.groupingBy(tabell -> (Class<?>) tabell.getDataKlasse()));
        tabellerPerRecord.forEach((recordKlasse, tabellerForRecord) ->
            assertThat(tabellerForRecord)
                .as("Skjema for %s", recordKlasse.getSimpleName())
                .extracting(BigQueryTabell::getSkjema)
                .containsOnly(tabellerForRecord.getFirst().getSkjema()));
    }

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"UNGDOMSYTELSE", "AKTIVITETSPENGER"})
    void skal_støtte_ungdomsytelse_og_aktivitetspenger(FagsakYtelseType ytelse) {
        assertThat(YtelseStatistikk.erStøttet(ytelse)).isTrue();
        assertThat(YtelseStatistikk.validerStøttet(ytelse)).isEqualTo(ytelse);
    }

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"OBSOLETE", "UDEFINERT"})
    void skal_avvise_ytelse_som_ikke_er_støttet(FagsakYtelseType ytelse) {
        assertThat(YtelseStatistikk.erStøttet(ytelse)).isFalse();
        assertThatThrownBy(() -> YtelseStatistikk.validerStøttet(ytelse)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> YtelseStatistikk.finn(ytelseStatistikk, ytelse)).isInstanceOf(IllegalArgumentException.class);
    }

    private List<BigQueryTabell<?>> alleTabeller(FagsakYtelseType ytelse) {
        YtelseStatistikk statistikk = YtelseStatistikk.finn(ytelseStatistikk, ytelse);
        return Stream.of(
                hyppigRapporterteTabeller(statistikk).stream(),
                tabeller(statistikk.hentDagligRapporterte()).stream(),
                Stream.<BigQueryTabell<?>>of(statistikk.uttalelseTabell(), statistikk.kontrollertePerioderTabell()))
            .flatMap(it -> it)
            .distinct()
            .toList();
    }

    private static List<BigQueryTabell<?>> hyppigRapporterteTabeller(YtelseStatistikk statistikk) {
        return tabeller(statistikk.hentHyppigRapporterte(LocalDateTime.now().minusHours(1)));
    }

    private static List<BigQueryTabell<?>> tabeller(List<Tuple<BigQueryTabell<?>, Collection<?>>> metrikker) {
        return metrikker.stream().<BigQueryTabell<?>>map(Tuple::getElement1).toList();
    }
}
