package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPeriode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestRepositories;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils.*;

public class AktivitetspengerEndringInntektScenarioer {

    /**
     * Enkel reduksjon: inntekt på 10 000 kr andre måned.
     */
    public static AktivitetspengerTestScenario endringMedInntektPå10k(LocalDate fom) {
        var registerInntektTimeline = new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(
                fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                BigDecimal.valueOf(10000))));

        return endringMedInntekt(fom, registerInntektTimeline);
    }

    /**
     * Reduksjon over flere måneder: inntekt på 10 000 kr i andre og tredje måned.
     */
    public static AktivitetspengerTestScenario endringMedInntektPå10kFlereMnd(LocalDate fom) {
        var registerInntektTimeline = new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(
                fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()),
                BigDecimal.valueOf(10000))));

        return endringMedInntekt(fom, registerInntektTimeline);
    }

    /**
     * Ingen utbetaling pga for mye inntekt: 23 000 kr andre måned.
     */
    public static AktivitetspengerTestScenario endringMedInntektIngenUtbetaling(LocalDate fom) {
        LocalDate førsteIMåneden = fom.withDayOfMonth(1);
        var registerInntektTimeline = new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(
                førsteIMåneden.plusMonths(1),
                førsteIMåneden.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                BigDecimal.valueOf(23000))));

        return endringMedInntekt(fom, registerInntektTimeline);
    }

    /**
     * Kombinasjon: ingen utbetaling (1. mnd) og redusert utbetaling (2. mnd).
     */
    public static AktivitetspengerTestScenario endringInntektRedusertOgIngenUtbetaling(LocalDate fom) {
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

        return endringMedInntekt(fom, registerInntektTimeline);
    }

    /**
     * Full utbetaling uten reduksjon: rapportert inntekt 10 000 kr men register og fastsatt er 0 kr.
     */
    public static AktivitetspengerTestScenario endring10000KrInntekt0KrRegisterInntekt_0krFastsatt(LocalDate fom) {
        BigDecimal rapportertInntekt = BigDecimal.valueOf(10000);

        var kontrollerInntektPerioder = new LocalDateTimeline<>(
            List.of(new LocalDateSegment<>(
                fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()),
                new KontrollerInntektHolder(
                    BigDecimal.ZERO,
                    rapportertInntekt,
                    BigDecimal.ZERO,
                    true)
            )));

        return endringMedKontrollertInntekt(fom, fom.plusWeeks(52).minusDays(1), kontrollerInntektPerioder);
    }

    private static AktivitetspengerTestScenario endringMedInntekt(LocalDate fom, LocalDateTimeline<BigDecimal> registerInntektTimeline) {
        var kontrollertInntektTidslinje = registerInntektTimeline.mapValue(
            KontrollerInntektHolder::forRegisterInntekt);
        return endringMedKontrollertInntekt(fom, fom.plusWeeks(52).minusDays(1), kontrollertInntektTidslinje);
    }

    private static AktivitetspengerTestScenario endringMedKontrollertInntekt(LocalDate fom, LocalDate tom, LocalDateTimeline<KontrollerInntektHolder> kontrollertInntektTidslinje) {
        var p = new LocalDateInterval(fom, tom);

        var satsGrunnlag = lavSatsBuilder(fom).build();
        var satsPeriode = new AktivitetspengerSatsPeriode(p, satsGrunnlag);

        var satsperioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tom, satsPeriode)
        ));

        var satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tom, satsGrunnlag)
        ));

        var beregningsgrunnlag = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, null, lagBeregningsgrunnlag(fom))
        ));

        var tilkjentPeriode = new LocalDateInterval(kontrollertInntektTidslinje.getMinLocalDate(), kontrollertInntektTidslinje.getMaxLocalDate());
        var satserTidslinje = lagSatserTidslinje(satsGrunnlagTidslinje, beregningsgrunnlag);
        var tilkjentYtelsePerioder = tilkjentYtelsePerioderMedReduksjon(satserTidslinje, tilkjentPeriode, kontrollertInntektTidslinje);

        var kontrollerInntektPerioder = kontrollerInntektFraHolder(p, kontrollertInntektTidslinje);

        var triggere = new HashSet<Trigger>();
        triggere.add(new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, DatoIntervallEntitet.fra(tilkjentPeriode.getFomDato(), tilkjentPeriode.getTomDato().with(TemporalAdjusters.lastDayOfMonth()))));

        LocalDate fødselsdato = fom.minusYears(20);

        return new AktivitetspengerTestScenario(
            DEFAULT_NAVN,
            List.of(new Periode(fom, tom)),
            satsperioder,
            beregningsgrunnlag,
            tilkjentYtelsePerioder,
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            triggere,
            Collections.emptyList(),
            null,
            kontrollerInntektPerioder);
    }

    public static Behandling lagBehandlingMedAksjonspunktKontrollerInntekt(
        AktivitetspengerTestScenario testScenario,
        AktivitetspengerTestRepositories repositories) {

        AktivitetspengerTestScenarioBuilder scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(testScenario);

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);

        AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();
        aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
        var aksjonspunkt = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
        aksjonspunktTestSupport.setTilUtført(aksjonspunkt, "utført");

        repositories.behandlingAnsvarligRepository().setAnsvarligSaksbehandler(
            behandling.getId(), BehandlingDel.SENTRAL, SAKSBEHANDLER1_IDENT);
        repositories.behandlingAnsvarligRepository().setAnsvarligBeslutter(
            behandling.getId(), BehandlingDel.SENTRAL, BESLUTTER_IDENT);

        behandling.avsluttBehandling();
        return behandling;
    }
}
