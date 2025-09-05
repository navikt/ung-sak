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
import java.util.HashSet;
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
            fom.withDayOfMonth(1).plusMonths(3).with(TemporalAdjusters.lastDayOfMonth()));

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
            Collections.emptyList(),
            null);
    }

    /**
     * Kombinasjon - endrer startdato og blir 25 år
     *
     **/
    public static UngTestScenario kombinasjon_endringStartDatoOgEndringHøySats(LocalDate fødselsdato, LocalDate nyStartdato, LocalDateInterval opprinneligProgramPeriode) {
        UngTestScenario ungTestScenario = EndringProgramPeriodeScenarioer.endringStartdato(nyStartdato, opprinneligProgramPeriode);


        var tjuvefemårsdag = fødselsdato.plusYears(25);
        var fom = tjuvefemårsdag.with(TemporalAdjusters.firstDayOfMonth()).minusMonths(3);

        var programPeriode = ungTestScenario.programPerioder().get(0).getPeriode();

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(programPeriode.getFomDato(), tjuvefemårsdag.minusDays(1), BrevScenarioerUtils.lavSatsBuilder(fom).build()),
            new LocalDateSegment<>(tjuvefemårsdag, programPeriode.getTomDato(), BrevScenarioerUtils.høySatsBuilder(fom).build())
        ));

        var triggere = new HashSet<>(ungTestScenario.behandlingTriggere());
        triggere.add(
            new Trigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DatoIntervallEntitet.fra(tjuvefemårsdag, programPeriode.getTomDato()))
        );

        return new UngTestScenario(
            ungTestScenario.navn(),
            ungTestScenario.programPerioder(),
            satser,
            ungTestScenario.uttakPerioder(),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            ungTestScenario.aldersvilkår(),
            ungTestScenario.ungdomsprogramvilkår(),
            fødselsdato,
            ungTestScenario.søknadStartDato(),
            triggere,
            ungTestScenario.barn(),
            null);
    }

    public static UngTestScenario endringStartdatoOgOpphør(LocalDateInterval opprinneligProgramPeriode, LocalDate nyStartdato, LocalDate sluttdato) {
        UngTestScenario ungTestScenario = EndringProgramPeriodeScenarioer.endringStartdato(nyStartdato, opprinneligProgramPeriode);

        var fom = opprinneligProgramPeriode.getFomDato();
        var fagsakPeriode = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
        LocalDate nyStartDato = ungTestScenario.programPerioder().get(0).getPeriode().getFomDato();
        var nyProgramPeriode = new LocalDateInterval(nyStartDato, sluttdato);
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(nyStartDato, fagsakPeriode.getTomDato(), BrevScenarioerUtils.lavSatsBuilder(fom).build())
        ));

        var triggere = new HashSet<>(ungTestScenario.behandlingTriggere());
        triggere.add(new Trigger(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER, DatoIntervallEntitet.fra(opprinneligProgramPeriode.getFomDato(), sluttdato)));
        triggere.add(new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, DatoIntervallEntitet.fra(sluttdato.plusDays(1), fagsakPeriode.getTomDato())));


        return new UngTestScenario(
            ungTestScenario.navn(),
            List.of(new UngdomsprogramPeriode(nyProgramPeriode.getFomDato(), nyProgramPeriode.getTomDato())),
            satser,
            BrevScenarioerUtils.uttaksPerioder(nyProgramPeriode),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, nyProgramPeriode),
            new LocalDateTimeline<>(fagsakPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(nyProgramPeriode, Utfall.OPPFYLT),
                new LocalDateSegment<>(sluttdato.plusDays(1), fagsakPeriode.getTomDato(), Utfall.IKKE_OPPFYLT)
            )

            ),
            ungTestScenario.fødselsdato(),
            ungTestScenario.søknadStartDato(),
            triggere,
            ungTestScenario.barn(),
            null);
    }


    /**
     * Kombinasjon - førstegangsinnvilgelse og fødsel av barn
     *
     **/
    public static UngTestScenario kombinasjon_førstegangsBehandlingOgBarn(LocalDate fom) {
        UngTestScenario ungTestScenario = FørstegangsbehandlingScenarioer.innvilget19år(fom);
        var barnFødselsdato = fom.withDayOfMonth(1).plusMonths(1).withDayOfMonth(15);

        var programPeriode = ungTestScenario.programPerioder().get(0).getPeriode();

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, barnFødselsdato.minusDays(1), BrevScenarioerUtils.lavSatsBuilder(fom).build()),
            new LocalDateSegment<>(barnFødselsdato, programPeriode.getTomDato(), BrevScenarioerUtils.lavSatsMedBarnBuilder(barnFødselsdato, 1).build())
        ));


        var triggere = new HashSet<>(ungTestScenario.behandlingTriggere());
        triggere.add(
            new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DatoIntervallEntitet.fra(barnFødselsdato, programPeriode.getTomDato()))
        );

        return new UngTestScenario(
            ungTestScenario.navn(),
            ungTestScenario.programPerioder(),
            satser,
            ungTestScenario.uttakPerioder(),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            ungTestScenario.aldersvilkår(),
            ungTestScenario.ungdomsprogramvilkår(),
            fom.minusYears(19).plusDays(42),
            ungTestScenario.søknadStartDato(),
            triggere,
            List.of(
                BrevScenarioerUtils.lagBarn(barnFødselsdato)
            ),
            null);
    }
}
