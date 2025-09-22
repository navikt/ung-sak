package no.nav.ung.sak.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.trigger.Trigger;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class EndringInntektScenarioer {
    /**
     * 19 år ungdom med full ungdomsperiode som rapporterer inntekt andre måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntektPå10k_19år(LocalDate fom) {
        var rapportertInntektTimeline = new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(
                fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                BigDecimal.valueOf(10000))));

        return endringMedInntekt_19år(fom, rapportertInntektTimeline);
    }

    /**
     * 19 år ungdom med full ungdomsperiode som rapporterer inntekt andre og tredje måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntektPå10k_flere_mnd_19år(LocalDate fom) {
        var rapportertInntektTimeline = new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(
                fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()),
                BigDecimal.valueOf(10000))));

        return endringMedInntekt_19år(fom, rapportertInntektTimeline);
    }

    /**
     * 19 år ungdom med full ungdomsperiode som rapporterer inntekt andre og tredje måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntektPå10k_utenom_mnd_2(LocalDate fom) {
        LocalDate førsteIMåneden = fom.withDayOfMonth(1);
        var rapportertInntektTimeline = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(1),
                    førsteIMåneden.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.valueOf(10000)),
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(2),
                    førsteIMåneden.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.ZERO),
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(3),
                    førsteIMåneden.plusMonths(3).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.valueOf(10000))

            ));

        return endringMedInntekt_19år(fom, rapportertInntektTimeline);
    }


    /**
     * 19 år ungdom med full ungdomsperiode som rapporterer inntekt andre og tredje måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntekt0krUtbetaling(LocalDate fom) {
        LocalDate førsteIMåneden = fom.withDayOfMonth(1);
        var rapportertInntektTimeline = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(1),
                    førsteIMåneden.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.valueOf(23000))
            ));

        return endringMedInntekt_19år(fom, rapportertInntektTimeline);
    }

    /**
     * 19 år ungdom med full ungdomsperiode som rapporterer inntekt andre og tredje måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntektReduksjonOgIngenUtbetalingKombinasjon(LocalDate fom) {
        LocalDate førsteIMåneden = fom.withDayOfMonth(1);
        var rapportertInntektTimeline = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(1),
                    førsteIMåneden.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.valueOf(10000)),
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(2),
                    førsteIMåneden.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.valueOf(23000)), //Vil gi 0 kr utbetaling
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(3),
                    førsteIMåneden.plusMonths(3).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.valueOf(10000))

            ));

        return endringMedInntekt_19år(fom, rapportertInntektTimeline);
    }

    /**
     * 19 år ungdom med full ungdomsperiode uten inntekt og rapporterer ingen inntekt
     */
    public static UngTestScenario endring0KrInntekt_19år(LocalDate fom) {
        var rapportertInntektTimeline = new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(
                fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                BigDecimal.ZERO)));


        return endringMedInntekt_19år(fom, rapportertInntektTimeline);
    }

    static UngTestScenario endringMedInntekt_19år(LocalDate fom, LocalDateTimeline<BigDecimal> rapportertInntektTimeline) {
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        var sats = BrevScenarioerUtils.lavSatsBuilder(fom).build();
        var satser = new LocalDateTimeline<>(p, sats);

        var tilkjentPeriode = new LocalDateInterval(rapportertInntektTimeline.getMinLocalDate(), rapportertInntektTimeline.getMaxLocalDate());
        var satserPrMåned = BrevScenarioerUtils.splitPrMåned(satser);
        var tilkjentYtelsePerioder = BrevScenarioerUtils.tilkjentYtelsePerioderMedReduksjon(satserPrMåned, tilkjentPeriode, rapportertInntektTimeline);


        var opptjening = OppgittOpptjeningBuilder.ny();

        rapportertInntektTimeline.forEach(it ->
            opptjening.leggTilOppgittArbeidsforhold(OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
                .medInntekt(it.getValue())
                .medPeriode(DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            ));

        var triggere = HashSet.<Trigger>newHashSet(2);
        triggere.add(new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, DatoIntervallEntitet.fra(tilkjentPeriode)));
        rapportertInntektTimeline.filterValue(it -> it.compareTo(BigDecimal.ZERO) > 0)
            .forEach(it -> triggere.add(new Trigger(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, DatoIntervallEntitet.fra(it.getLocalDateInterval()))));

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            satser,
            BrevScenarioerUtils.uttaksPerioder(p),
            tilkjentYtelsePerioder,
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(p.getFomDato()),
            triggere,
            Collections.emptyList(), null);
    }

    public static Behandling lagBehandlingMedAksjonspunktKontrollerInntekt(UngTestScenario ungTestscenario, UngTestRepositories ungTestRepositories1) {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestscenario);

        scenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);

        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories1);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
        new AksjonspunktTestSupport().setTilUtført(aksjonspunkt, "utført");
        BehandlingRepository behandlingRepository = ungTestRepositories1.repositoryProvider().getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }

    @Test
    void testTilkjentYtelseReduksjonScenario() {
        var scenario = EndringInntektScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));
        var andreMåned = scenario.tilkjentYtelsePerioder().getSegment(new LocalDateInterval(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)));

        assertThat(andreMåned.getFom()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(andreMåned.getTom()).isEqualTo(LocalDate.of(2025, 1, 31));

        //23 virkningsdager i januar 2025 med lav dagsats på 649,08. Rapportert inntekt er 10 000kr
        TilkjentYtelseVerdi t = andreMåned.getValue();
        assertThat(t.uredusertBeløp()).isEqualByComparingTo("14928.8369336998"); //649,08 * 23
        assertThat(t.reduksjon()).isEqualByComparingTo("6600"); //66% av 10 0000
        assertThat(t.dagsats()).isEqualByComparingTo("362"); //649 - ((6600/22)  )
        assertThat(t.redusertBeløp()).isEqualByComparingTo("8328.8369336998"); // 14928.84 - 6600
        assertThat(t.utbetalingsgrad()).isEqualTo(56); // 8328.84 / 14928.84 * 100

    }
}
