package no.nav.ung.sak.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.UngTestRepositories;
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
     * 19 år ungdom med full ungdomsperiode som har inntekt andre måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntektPå10k_19år(LocalDate fom) {
        var registerInntektTimeline = new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(
                fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                BigDecimal.valueOf(10000))));

        return endringMedInntekt_19år(fom, registerInntektTimeline);
    }

    /**
     * 19 år ungdom med full ungdomsperiode som har inntekt andre og tredje måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntektPå10k_flere_mnd_19år(LocalDate fom) {
        var registerInntektTimeline = new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(
                fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()),
                BigDecimal.valueOf(10000))));

        return endringMedInntekt_19år(fom, registerInntektTimeline);
    }

    /**
     * 19 år ungdom med full ungdomsperiode som har inntekt andre og tredje måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntektPå10k_utenom_mnd_2(LocalDate fom) {
        LocalDate førsteIMåneden = fom.withDayOfMonth(1);
        var registerInntektTimeline = new LocalDateTimeline<>(
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

        return endringMedInntekt_19år(fom, registerInntektTimeline);
    }


    /**
     * 19 år ungdom med full ungdomsperiode som har inntekt andre og tredje måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntektIngenUtbetaling(LocalDate fom) {
        LocalDate førsteIMåneden = fom.withDayOfMonth(1);
        var registerInntektTimeline = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(1),
                    førsteIMåneden.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.valueOf(23000))
            ));

        return endringMedInntekt_19år(fom, registerInntektTimeline);
    }

    /**
     * 19 år ungdom med
     * 1. mnd: 0 kr utbetaling med for høy inntekt
     * 2. mnd: redusert utbetaling
     */
    public static UngTestScenario endringInntektRedusertOgIngenUtbetaling(LocalDate fom) {
        LocalDate førsteIMåneden = fom.withDayOfMonth(1);
        var registerInntektTimeline = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(1),
                    førsteIMåneden.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.valueOf(23000)),
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(2),
                    førsteIMåneden.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.valueOf(10000))
            ));

        return endringMedInntekt_19år(fom, registerInntektTimeline);
    }

    /**
     * 19 år ungdom med
     * 1. mnd: 0 kr utbetaling med for høy inntekt
     * 2. mnd: redusert utbetaling
     * 3. mnd: ingen reduksjon pga ingen inntekt
     * 4. mnd: 0 kr utbetaling pga for høy inntekt
     * 5. mnd: redusert utbetaling
     */
    public static UngTestScenario endringInntektAlleKombinasjoner(LocalDate fom) {
        LocalDate førsteIMåneden = fom.withDayOfMonth(1);
        var registerInntektTimeline = new LocalDateTimeline<>(
            List.of(
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(1),
                    førsteIMåneden.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.valueOf(23000)),
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(2),
                    førsteIMåneden.plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.valueOf(10000)),
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(3),
                    førsteIMåneden.plusMonths(3).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.ZERO),
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(4),
                    førsteIMåneden.plusMonths(4).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.valueOf(23000)),
                new LocalDateSegment<>(
                    førsteIMåneden.plusMonths(5),
                    førsteIMåneden.plusMonths(5).with(TemporalAdjusters.lastDayOfMonth()),
                    BigDecimal.valueOf(10000))

            ));

        return endringMedInntekt_19år(fom, registerInntektTimeline);
    }

    /**
     * 19 år ungdom med full ungdomsperiode uten inntekt og har ingen inntekt
     */
    public static UngTestScenario endring0KrInntekt_19år(LocalDate fom) {
        var registerInntektTimeline = new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(
                fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                BigDecimal.ZERO)));


        return endringMedInntekt_19år(fom, registerInntektTimeline);
    }

    /**
     * 19 år ungdom med full ungdomsperiode har inntekt men har 0 kr i register og får derfor 0 kr i fastsatt
     */
    public static UngTestScenario endring10000KrInntekt0KrRegisterInntekt_0krFastsatt(LocalDate fom) {
        BigDecimal rapportertInntekt = BigDecimal.valueOf(10000);

        var kontrollerInntektPerioder = new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(
                fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                new BrevScenarioerUtils.KontrollerInntektHolder(
                    BigDecimal.ZERO,
                    rapportertInntekt,
                    BigDecimal.ZERO,
                    true)
            )));

        return endringMedInntekt_19år_med_kontroll(fom, kontrollerInntektPerioder);
    }

    /**
     * 19 år ungdom med full ungdomsperiode har inntekt men har 10000 kr i register men rapporterer 0 kr, saksbehandler fastsetter 0 kr
     */
    public static UngTestScenario endring0KrRapportert10000KrRegister0krFastsatt(LocalDate fom) {
        BigDecimal registerInntekt = BigDecimal.valueOf(10000);

        var kontrollerInntektPerioder = new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(
                fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                new BrevScenarioerUtils.KontrollerInntektHolder(
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    registerInntekt,
                    true)
            )));

        return endringMedInntekt_19år_med_kontroll(fom, kontrollerInntektPerioder);
    }

    static UngTestScenario endringMedInntekt_19år(LocalDate fom, LocalDateTimeline<BigDecimal> registerInntektTimeline) {
        var kontrollertInntektTidslinje = registerInntektTimeline.mapValue(
            BrevScenarioerUtils.KontrollerInntektHolder::forRegisterInntekt);
        return endringMedInntekt_19år_med_kontroll(fom, kontrollertInntektTidslinje);
    }

    static UngTestScenario endringMedInntekt_19år_med_kontroll(LocalDate fom, LocalDateTimeline<BrevScenarioerUtils.KontrollerInntektHolder> kontrollertInntektTidslinje) {
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        var satser = BrevScenarioerUtils.lavSatsBuilder(p);

        var tilkjentPeriode = new LocalDateInterval(kontrollertInntektTidslinje.getMinLocalDate(), kontrollertInntektTidslinje.getMaxLocalDate());
        var satserPrMåned = BrevScenarioerUtils.splitPrMåned(satser);
        var tilkjentYtelsePerioder = BrevScenarioerUtils.tilkjentYtelsePerioderMedReduksjon(satserPrMåned, tilkjentPeriode, kontrollertInntektTidslinje);

        var kontrollerInntektPerioder = BrevScenarioerUtils.kontrollerInntektFraHolder(p, tilkjentYtelsePerioder, kontrollertInntektTidslinje);

        var opptjening = OppgittOpptjeningBuilder.ny();

        kontrollerInntektPerioder.forEach(it ->
            opptjening.leggTilOppgittArbeidsforhold(OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
                .medInntekt(it.getValue().getRapportertInntekt())
                .medPeriode(DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            ));

        var triggere = HashSet.<Trigger>newHashSet(2);
        triggere.add(new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, DatoIntervallEntitet.fra(tilkjentPeriode)));
        kontrollerInntektPerioder.filterValue(it -> it.getInntekt().compareTo(BigDecimal.ZERO) > 0)
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
            Collections.emptyList(), null,
            kontrollerInntektPerioder);
    }

    public static Behandling lagBehandlingMedAksjonspunktKontrollerInntekt(UngTestScenario ungTestscenario, UngTestRepositories ungTestRepositories) {
        var behandling = BrevScenarioerUtils.lagInnvilgetBehandling(ungTestscenario, ungTestRepositories);

        BehandlingRepository behandlingRepository = ungTestRepositories.repositoryProvider().getBehandlingRepository();
        BrevScenarioerUtils.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, behandling, BrevScenarioerUtils.SAKSBEHANDLER1_IDENT, behandlingRepository);
        BrevScenarioerUtils.leggTilBeslutter(behandling, behandlingRepository);

        return behandling;
    }

    public static Behandling lagBehandlingMedAksjonspunktVurderFeilutbetaling(UngTestScenario ungTestscenario, UngTestRepositories ungTestRepositories) {
        var behandling = BrevScenarioerUtils.lagInnvilgetBehandling(ungTestscenario, ungTestRepositories);

        BehandlingRepository behandlingRepository = ungTestRepositories.repositoryProvider().getBehandlingRepository();
        BrevScenarioerUtils.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_FEILUTBETALING, behandling, BrevScenarioerUtils.SAKSBEHANDLER1_IDENT, behandlingRepository);
        BrevScenarioerUtils.leggTilBeslutter(behandling, behandlingRepository);
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
        assertThat(t.utbetalingsgrad()).isEqualByComparingTo("55.7781201849"); // 8328.84 / 14928.84 * 100

    }
}
