package no.nav.ung.sak.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.trigger.Trigger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class KombinasjonScenarioer {


    /**
     * Kombinasjon - rapporterer 10 000kr inntekt 1 måned etter fom + fødsel av barn
     *
     **/
    public static UngTestScenario kombinasjon_endringMedInntektOgFødselAvBarn(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        var barnFødselsdato = fom.withDayOfMonth(1).plusMonths(1).withDayOfMonth(15);

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, barnFødselsdato.minusDays(1), BrevScenarioerUtils.lavSatsBuilder(fom).build()),
            new LocalDateSegment<>(barnFødselsdato, p.getTomDato(), BrevScenarioerUtils.lavSatsMedBarnBuilder(barnFødselsdato, 1).build())
        ));


        var rapportertInntektPeriode = new LocalDateInterval(fom.withDayOfMonth(1).plusMonths(1),
            fom.withDayOfMonth(1).plusMonths(1)
                .with(TemporalAdjusters.lastDayOfMonth()));

        var satserPrMåned = BrevScenarioerUtils.splitPrMåned(satser);
        var rapportertInntektTimeline = BrevScenarioerUtils.splitPrMåned(new LocalDateTimeline<>(rapportertInntektPeriode, BigDecimal.valueOf(10000)));
        var tilkjentYtelsePerioder = BrevScenarioerUtils.tilkjentYtelsePerioderMedReduksjon(satserPrMåned, rapportertInntektPeriode, rapportertInntektTimeline);

        var opptjening = OppgittOpptjeningBuilder.ny();

        rapportertInntektTimeline.forEach(it ->
            opptjening.leggTilOppgittArbeidsforhold(OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
                .medInntekt(it.getValue())
                .medPeriode(DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            ));


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
            Set.of(
                new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, DatoIntervallEntitet.fra(rapportertInntektPeriode)),
                new Trigger(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, DatoIntervallEntitet.fra(rapportertInntektPeriode)),
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DatoIntervallEntitet.fra(barnFødselsdato, p.getTomDato()))
            ),
            null,
            List.of(
                BrevScenarioerUtils.lagBarn(barnFødselsdato)
            ), null);
    }


    /**
     * Kombinasjon - rapporterer 10 000kr inntekt 3 måned etter fom + blir 25 år
     *
     **/
    public static UngTestScenario kombinasjon_endringMedInntektOgEndringHøySats(LocalDate fødselsdato) {
        var tjuvefemårsdag = fødselsdato.plusYears(25);
        var fom = tjuvefemårsdag.with(TemporalAdjusters.firstDayOfMonth()).minusMonths(3);

        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tjuvefemårsdag.minusDays(1), BrevScenarioerUtils.lavSatsBuilder(fom).build()),
            new LocalDateSegment<>(tjuvefemårsdag, p.getTomDato(), BrevScenarioerUtils.høySatsBuilder(fom).build())
        ));


        var rapportertInntektPeriode = new LocalDateInterval(fom.withDayOfMonth(1).plusMonths(3),
            fom.withDayOfMonth(1).plusMonths(3)
                .with(TemporalAdjusters.lastDayOfMonth()));

        var satserPrMåned = BrevScenarioerUtils.splitPrMåned(satser);
        var rapportertInntektTimeline = BrevScenarioerUtils.splitPrMåned(new LocalDateTimeline<>(rapportertInntektPeriode, BigDecimal.valueOf(10000)));
        var tilkjentYtelsePerioder = BrevScenarioerUtils.tilkjentYtelsePerioderMedReduksjon(satserPrMåned, rapportertInntektPeriode, rapportertInntektTimeline);

        var opptjening = OppgittOpptjeningBuilder.ny();

        rapportertInntektTimeline.forEach(it ->
            opptjening.leggTilOppgittArbeidsforhold(OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
                .medInntekt(it.getValue())
                .medPeriode(DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            ));


        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            programPerioder,
            satser,
            BrevScenarioerUtils.uttaksPerioder(p),
            tilkjentYtelsePerioder,
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            List.of(p.getFomDato()),
            Set.of(
                new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, DatoIntervallEntitet.fra(rapportertInntektPeriode)),
                new Trigger(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, DatoIntervallEntitet.fra(rapportertInntektPeriode)),
                new Trigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DatoIntervallEntitet.fra(tjuvefemårsdag, p.getTomDato()))
            ),
            null,
            Collections.emptyList(),
            null);
    }
}
