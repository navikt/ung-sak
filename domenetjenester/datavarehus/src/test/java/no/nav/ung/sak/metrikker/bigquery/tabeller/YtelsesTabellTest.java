package no.nav.ung.sak.metrikker.bigquery.tabeller;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.metrikker.bigquery.tabeller.aksjonspunkt.AksjonspunktRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsresultat.BehandslingsresultatStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingstatus.BehandlingStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.behandlingsårsak.BehandlingÅrsakRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.etterlysning.EtterlysningRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.fagsakstatus.FagsakStatusRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.inntektskontroll.KontrollertePerioderRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.personopplysninger.AlderOgKjønnRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.sats.SatsStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.soknad.SøknadStatistikkRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.ungdomsprogram.DagerIProgrammetRecord;
import no.nav.ung.sak.metrikker.bigquery.tabeller.uttalelse.UttalelseRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YtelsesTabellTest {

    private static final List<YtelsesTabell<?>> ALLE_YTELSESTABELLER = List.of(
        FagsakStatusRecord.FAGSAK_STATUS_TABELLER,
        BehandlingStatusRecord.BEHANDLING_STATUS_TABELLER,
        AksjonspunktRecord.AKSJONSPUNKT_TABELLER,
        SatsStatistikkRecord.SATS_STATISTIKK_TABELLER,
        BehandslingsresultatStatistikkRecord.BEHANDLINGSRESULTAT_STATISTIKK_TABELLER,
        EtterlysningRecord.ETTERLYSNING_TABELLER,
        UttalelseRecord.UTTALELSE_TABELLER,
        BehandlingÅrsakRecord.BEHANDLING_ÅRSAK_TABELLER,
        AlderOgKjønnRecord.ALDER_OG_KJØNN_TABELLER,
        KontrollertePerioderRecord.KONTROLLERTE_PERIODER_TABELLER
    );

    @Test
    void skal_beholde_eksisterende_tabell_for_ungdomsytelsen_og_egen_tabell_for_aktivitetspenger() {
        var tabeller = FagsakStatusRecord.FAGSAK_STATUS_TABELLER;

        assertThat(tabeller.forYtelse(FagsakYtelseType.UNGDOMSYTELSE)).isSameAs(FagsakStatusRecord.FAGSAK_STATUS_TABELL_V2);
        assertThat(tabeller.forYtelse(FagsakYtelseType.AKTIVITETSPENGER).getTabellnavn()).isEqualTo("aktivitetspenger_fagsak_status");
    }

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"OBSOLETE", "UDEFINERT"})
    void skal_ikke_støtte_utgåtte_eller_udefinerte_ytelser(FagsakYtelseType ytelse) {
        assertThat(YtelsesTabell.erStøttet(ytelse)).isFalse();
        assertThatThrownBy(() -> YtelsesTabell.validerStøttet(ytelse)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> FagsakStatusRecord.FAGSAK_STATUS_TABELLER.forYtelse(ytelse))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(value = FagsakYtelseType.class, names = {"UNGDOMSYTELSE", "AKTIVITETSPENGER"})
    void skal_støtte_ungdomsytelse_og_aktivitetspenger(FagsakYtelseType ytelse) {
        assertThat(YtelsesTabell.erStøttet(ytelse)).isTrue();
        assertThat(YtelsesTabell.validerStøttet(ytelse)).isEqualTo(ytelse);
    }

    @Test
    void skal_ha_unike_tabellnavn_på_tvers_av_alle_tabeller() {
        List<String> tabellnavn = Stream.concat(
                ALLE_YTELSESTABELLER.stream().flatMap(it -> it.alle().stream()),
                Stream.of(SøknadStatistikkRecord.AKTIVITETSPENGER_SØKNAD_TABELL, DagerIProgrammetRecord.DAGER_I_PROGRAMMET_LØPENDE_BIG_QUERY_TABELL))
            .map(BigQueryTabell::getTabellnavn)
            .toList();

        assertThat(tabellnavn).doesNotHaveDuplicates();
    }

    @Test
    void skal_ha_aktivitetspenger_prefiks_og_samme_skjema_som_ungdomsytelsen() {
        for (YtelsesTabell<?> tabeller : ALLE_YTELSESTABELLER) {
            var ung = tabeller.forYtelse(FagsakYtelseType.UNGDOMSYTELSE);
            var akt = tabeller.forYtelse(FagsakYtelseType.AKTIVITETSPENGER);

            assertThat(akt.getTabellnavn()).startsWith("aktivitetspenger_");
            assertThat(akt.getSkjema()).isEqualTo(ung.getSkjema());
            assertThat(akt.getDataKlasse()).isEqualTo(ung.getDataKlasse());
        }
    }
}
