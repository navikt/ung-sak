package no.nav.ung.sak.formidling;


import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.uttak.Tid;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningVersjonType;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.behandlingslager.ytelse.sats.*;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTidslinje;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.ytelse.BeregnetSats;
import no.nav.ung.sak.ytelse.TilkjentYtelseBeregner;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class BrevScenarioer {

    public static final String DEFAULT_NAVN = "Ung Testesen";


    public static TestScenarioBuilder lagAvsluttetStandardBehandling(UngTestRepositories repositories) {
        UngTestScenario ungTestscenario = innvilget19år(LocalDate.of(2024, 12, 1));

        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestscenario);

        var behandling = scenarioBuilder.buildOgLagreMedUng(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return scenarioBuilder;
    }

    /**
     * 19 år ungdom med full ungdomsperiode, ingen inntektsgradering og ingen barn
     */
    public static UngTestScenario innvilget19år(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));

        var satser = new LocalDateTimeline<>(p, lavSatsBuilder(fom).build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), Tid.TIDENES_ENDE));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null,
            Collections.emptyList(), null);
    }

    /**
     * 19 år ungdom med full ungdomsperiode med 1 barn 15 dager etter fom, ingen inntektsgradering
     */
    public static UngTestScenario innvilget19årMedBarn15DagerEtterStartdato(LocalDate fom) {
        LocalDate barnFødselsdato = fom.plusDays(15);
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(p.getFomDato(), barnFødselsdato.minusDays(1), lavSatsBuilder(fom).build()),
            new LocalDateSegment<>(barnFødselsdato, p.getTomDato(), lavSatsMedBarnBuilder(barnFødselsdato, 1).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null,
            List.of(
                lagBarn(barnFødselsdato)
            ), null);
    }

    /**
     * Scenario med alle kombinasjoner:
     * Innvilget fom lenge før dagens dato
     * 24 år ungdom blir 25 år i mai. 2 barn født etter startdato der ene dør. Overgang av G-beløp i tillegg
     * Søker i mai slutten av mai med startdato 20 april. Får hele mai og april
     * Får 2 barn så dør ene barnet så overgang til 25 år.
     */
    public static UngTestScenario innvilget24MedAlleKombinasjonerFom21April2025() {
        LocalDate fom = LocalDate.of(2025, 4, 21);
        LocalDate barnFødselsdato = fom.plusDays(15);
        LocalDate barnDødsdato = barnFødselsdato.plusDays(4);
        LocalDate tjuvefemårsdato = barnDødsdato.plusDays(2);
        LocalDate fødselsdato = tjuvefemårsdato.minusYears(25);

        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(p.getFomDato(), barnFødselsdato.minusDays(1), lavSatsBuilder(fom).build()),
            new LocalDateSegment<>(barnFødselsdato, barnDødsdato.minusDays(1), lavSatsMedBarnBuilder(barnFødselsdato, 2).build()), //Får ny G
            new LocalDateSegment<>(barnDødsdato, tjuvefemårsdato.minusDays(1), lavSatsMedBarnBuilder(barnFødselsdato, 1).build()),
            new LocalDateSegment<>(tjuvefemårsdato, p.getTomDato(), høySatsBuilderMedBarn(barnFødselsdato, 1).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null,
            List.of(
                lagBarnMedDødsdato(barnFødselsdato, barnDødsdato),
                lagBarn(barnFødselsdato)
            ),
            null);
    }


    private static PersonInformasjon lagBarn(LocalDate barnFødselsdato) {
        return PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT).medPersonas().barn(AktørId.dummy(), barnFødselsdato).build();
    }

    private static PersonInformasjon lagBarnMedDødsdato(LocalDate barnFødselsdato, LocalDate barnDødsdato) {
        return PersonInformasjon.builder(PersonopplysningVersjonType.REGISTRERT).medPersonas().barn(AktørId.dummy(), barnFødselsdato).dødsdato(barnDødsdato).build();
    }


    /**
     * 27 år ungdom med full ungdomsperiode, ingen inntektsgradering og ingen barn, høy sats
     */
    public static UngTestScenario innvilget27år(LocalDate fom) {
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var satser = new LocalDateTimeline<>(p,
            høySatsBuilder(fom).build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(27).plusDays(42),
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null, Collections.emptyList(), null);
    }

    /**
     * 28 år ungdom med ungdomsprogramperiode fram til 29 år,
     * ingen inntektsgradering og ingen barn, høy sats
     */
    public static UngTestScenario innvilget29År(LocalDate fom, LocalDate fødselsdato) {
        var p = new LocalDateInterval(fom, fødselsdato.plusYears(29));

        var satser = new LocalDateTimeline<>(p, høySatsBuilder(fom).build());

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null, Collections.emptyList(), null);
    }

    /**
     * Søker bakover i tid med startdato lik første i måneden fylte 25 år.
     * Blir 25 etter 15 dager i programmet
     * Søker måneden etter fylte 25 år.
     * Så er 24 år ved startdato.
     * Får både lav og høy sats i førstegangsbehandlingen
     * ingen inntektsgradering og ingen barn
     */
    public static UngTestScenario innvilget24årBle25årførsteMåned(LocalDate fom) {
        LocalDate tjuvefemårsdag = fom.plusDays(15);
        LocalDate fødselsdato = tjuvefemårsdag.minusYears(25);
        LocalDate tom25årmnd = tjuvefemårsdag.with(TemporalAdjusters.lastDayOfMonth());
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tjuvefemårsdag.minusDays(1), lavSatsBuilder(p.getFomDato()).build()),
            new LocalDateSegment<>(tjuvefemårsdag, p.getTomDato(), høySatsBuilder(tjuvefemårsdag).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        LocalDateInterval tilkjentPeriode = new LocalDateInterval(fom, tom25årmnd);
        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser, tilkjentPeriode),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(p))), null, Collections.emptyList(), null);
    }


    /**
     * 19 år ungdom med full ungdomsperiode som rapporterer inntekt andre måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntektPå10k_19år(LocalDate fom) {
        return endringMedInntekt_19år(fom,
            new LocalDateInterval(fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(1)
                    .with(TemporalAdjusters.lastDayOfMonth())), 10000);
    }

    /**
     * 19 år ungdom med full ungdomsperiode som rapporterer inntekt andre og tredje måned på 10 000 kroner.
     * Se enhetstest i samme klasse for hvordan de ulike tilkjentytelse verdiene blir for måneden det er inntekt.
     */
    public static UngTestScenario endringMedInntektPå10k_flere_mnd_19år(LocalDate fom) {
        LocalDateInterval rapportertInntektPeriode = new LocalDateInterval(
            fom.withDayOfMonth(1).plusMonths(1),
            fom.withDayOfMonth(1).plusMonths(2).with(TemporalAdjusters.lastDayOfMonth()));

        return endringMedInntekt_19år(fom, rapportertInntektPeriode, 10000);
    }

    /**
     * 19 år ungdom med full ungdomsperiode uten inntekt og rapporterer ingen inntekt
     */
    public static UngTestScenario endring0KrInntekt_19år(LocalDate fom) {
        return endringMedInntekt_19år(fom,
            new LocalDateInterval(fom.withDayOfMonth(1).plusMonths(1),
                fom.withDayOfMonth(1).plusMonths(1)
                    .with(TemporalAdjusters.lastDayOfMonth())), null);
    }

    @NotNull
    private static UngTestScenario endringMedInntekt_19år(LocalDate fom, LocalDateInterval rapportertInntektPeriode, Integer rapportertInntektPrMåned) {
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        var sats = lavSatsBuilder(fom).build();
        var satser = new LocalDateTimeline<>(p, sats);

        var satserPrMåned = splitPrMåned(satser);
        var rapportertInntektTimeline = splitPrMåned(new LocalDateTimeline<>(rapportertInntektPeriode, rapportertInntektPrMåned != null ? BigDecimal.valueOf(rapportertInntektPrMåned) : BigDecimal.ZERO));
        var tilkjentYtelsePerioder = tilkjentYtelsePerioderMedReduksjon(satserPrMåned, rapportertInntektPeriode, rapportertInntektTimeline);


        var opptjening = OppgittOpptjeningBuilder.ny();

        rapportertInntektTimeline.forEach(it ->
            opptjening.leggTilOppgittArbeidsforhold(OppgittArbeidsforholdBuilder.ny()
                .medInntekt(it.getValue())
                .medPeriode(DatoIntervallEntitet.fra(it.getLocalDateInterval()))
            ));

        var triggere = HashSet.<Trigger>newHashSet(2);
        triggere.add(new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, DatoIntervallEntitet.fra(rapportertInntektPeriode)));
        if (rapportertInntektPrMåned != null) {
            triggere.add(new Trigger(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, DatoIntervallEntitet.fra(rapportertInntektPeriode)));
        }

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder,
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(p.getFomDato()),
            triggere,
            opptjening, Collections.emptyList(), null);
    }

    /**
     * 24 år blir 25 år etter 3 mnd i progrmmet og får overgang til høy sats
     */
    public static UngTestScenario endring25År(LocalDate fødselsdato) {
        var tjuvefemårsdag = fødselsdato.plusYears(25);
        var fom = tjuvefemårsdag.with(TemporalAdjusters.firstDayOfMonth()).minusMonths(3);

        var programPeriode = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(programPeriode.getFomDato(), tjuvefemårsdag.minusDays(1), lavSatsBuilder(fom).build()),
            new LocalDateSegment<>(tjuvefemårsdag, programPeriode.getTomDato(), høySatsBuilder(fom).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(programPeriode.getFomDato(), programPeriode.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(programPeriode),
            tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(programPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(programPeriode, Utfall.OPPFYLT),
            fødselsdato,
            List.of(programPeriode.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DatoIntervallEntitet.fra(tjuvefemårsdag, programPeriode.getTomDato()))), null, Collections.emptyList(), null);
    }

    /**
     * Endring barnetillegg. Får barn etter å ha fått innvilget programmet
     */
    public static UngTestScenario endringBarnetillegg(LocalDate fom, LocalDate barnFødselsdato) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, barnFødselsdato.minusDays(1), lavSatsBuilder(fom).build()),
            new LocalDateSegment<>(barnFødselsdato, p.getTomDato(), lavSatsMedBarnBuilder(barnFødselsdato, 1).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DatoIntervallEntitet.fra(barnFødselsdato, p.getTomDato()))), null,
            List.of(
                lagBarn(barnFødselsdato)
            ), null);
    }

    /**
     * Endring barnetillegg. Har allerede ett barn og får tvillinger
     */
    public static UngTestScenario endringBarnetilleggFlereBarn(LocalDate fom, LocalDate barnFødselsdato) {
        var p = new LocalDateInterval(fom, fom.plusYears(1));
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, barnFødselsdato.minusDays(1), lavSatsMedBarnBuilder(fom, 1).build()),
            new LocalDateSegment<>(barnFødselsdato, p.getTomDato(), lavSatsMedBarnBuilder(barnFødselsdato, 3).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(p.getFomDato(), p.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(p),
            tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(p.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DatoIntervallEntitet.fra(barnFødselsdato, p.getTomDato()))), null,
            List.of(
                lagBarn(barnFødselsdato.minusYears(5)),
                lagBarn(barnFødselsdato),
                lagBarn(barnFødselsdato)
            ), null);
    }


    /**
     * 24 år blir 25 år etter 3 mnd i progrmmet og får overgang til høy sats. Har barn fra før av
     */
    public static UngTestScenario endring25ÅrMedToBarn(LocalDate fødselsdato) {
        var tjuvefemårsdag = fødselsdato.plusYears(25);
        var fom = tjuvefemårsdag.with(TemporalAdjusters.firstDayOfMonth()).minusMonths(3);

        var programPeriode = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(programPeriode.getFomDato(), tjuvefemårsdag.minusDays(1), lavSatsMedBarnBuilder(fom, 2).build()),
            new LocalDateSegment<>(tjuvefemårsdag, programPeriode.getTomDato(), høySatsBuilderMedBarn(tjuvefemårsdag, 2).build())
        ));

        var programPerioder = List.of(new UngdomsprogramPeriode(programPeriode.getFomDato(), programPeriode.getTomDato()));

        return new UngTestScenario(
            DEFAULT_NAVN,
            programPerioder,
            satser,
            uttaksPerioder(programPeriode),
            tilkjentYtelsePerioder(satser, new LocalDateInterval(fom, fom.plusMonths(1).minusDays(1))),
            new LocalDateTimeline<>(programPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(programPeriode, Utfall.OPPFYLT),
            fødselsdato,
            List.of(programPeriode.getFomDato()),
            Set.of(new Trigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DatoIntervallEntitet.fra(tjuvefemårsdag, programPeriode.getTomDato()))),
            null,
            List.of(
                lagBarn(fom.minusYears(5))
            ), null);
    }


    /**
     *
     *  Opphør av programmet.
     *
     * @param opprinneligProgramPeriode - den perioden som opprinnelig ble innvilget
     * @param sluttdato - sluttdato
     * @return
     */
    public static UngTestScenario endringOpphør(LocalDateInterval opprinneligProgramPeriode, LocalDate sluttdato) {
        var fom = opprinneligProgramPeriode.getFomDato();
        var fagsakPeriode = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
        var nyProgramPeriode = new LocalDateInterval(fom, sluttdato);
        var satser = new LocalDateTimeline<>(List.of(
           new LocalDateSegment<>(fagsakPeriode.getFomDato(), fagsakPeriode.getTomDato(), lavSatsBuilder(fom).build())
        ));


        return new UngTestScenario(
            DEFAULT_NAVN,
            List.of(new UngdomsprogramPeriode(nyProgramPeriode.getFomDato(), nyProgramPeriode.getTomDato())),
            satser,
            uttaksPerioder(nyProgramPeriode),
            tilkjentYtelsePerioder(satser, nyProgramPeriode),
            new LocalDateTimeline<>(fagsakPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(nyProgramPeriode, Utfall.OPPFYLT),
                new LocalDateSegment<>(sluttdato.plusDays(1), fagsakPeriode.getTomDato(), Utfall.IKKE_OPPFYLT)
                )

            ),
            fom.minusYears(19).plusDays(42),
            List.of(opprinneligProgramPeriode.getFomDato()),
            Set.of(
                new Trigger(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER, DatoIntervallEntitet.fra(opprinneligProgramPeriode.getFomDato(), sluttdato)),
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, DatoIntervallEntitet.fra(sluttdato.plusDays(1), fagsakPeriode.getTomDato()))
            ),
            null,
            Collections.emptyList(),
            null);
    }

    /**
     *
     *  Innvilget med sluttdato
     *
     */
    public static UngTestScenario innvilget19årMedSluttdato(LocalDate startdato, LocalDate sluttdato) {
        var p = new LocalDateInterval(startdato, startdato.plusYears(1));
        var fagsakPeriode = new LocalDateInterval(startdato, startdato.plusWeeks(52).minusDays(1));
        var programperiode = new LocalDateInterval(startdato, sluttdato);
        var satser = new LocalDateTimeline<>(p, lavSatsBuilder(startdato).build());

        return new UngTestScenario(
            DEFAULT_NAVN,
            List.of(new UngdomsprogramPeriode(programperiode.getFomDato(), programperiode.getTomDato())),
            satser,
            uttaksPerioder(programperiode),
            tilkjentYtelsePerioder(satser, programperiode),
            new LocalDateTimeline<>(fagsakPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(programperiode, Utfall.OPPFYLT),
                new LocalDateSegment<>(sluttdato.plusDays(1), fagsakPeriode.getTomDato(), Utfall.IKKE_OPPFYLT)
            )

            ),
            startdato.minusYears(19).plusDays(42),
            List.of(startdato),
            Set.of(
                new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fra(startdato, sluttdato)),
                new Trigger(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER, DatoIntervallEntitet.fra(startdato, sluttdato))
            ),
            null,
            Collections.emptyList(),
            null);
    }

    /**
     * Har allerede opphørt, endrer opphørsdato
     */
    public static UngTestScenario endringSluttdato(LocalDate nySluttdato, LocalDateInterval opprinneligProgramPeriode) {
        if (nySluttdato.isEqual(opprinneligProgramPeriode.getTomDato())) {
            throw new IllegalArgumentException("Ny sluttdato er lik opprinnelig sluttdato");
        }

        var fagsakPeriode = new LocalDateInterval(opprinneligProgramPeriode.getFomDato(), opprinneligProgramPeriode.getFomDato().plusWeeks(52).minusDays(1));

        var fom = opprinneligProgramPeriode.getFomDato();

        var nyProgramPeriode = new LocalDateInterval(fom, nySluttdato);
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(opprinneligProgramPeriode.getFomDato(), opprinneligProgramPeriode.getTomDato(), lavSatsBuilder(fom).build())
        ));

        boolean flyttetBakover = nySluttdato.isBefore(opprinneligProgramPeriode.getTomDato());

        return new UngTestScenario(
            DEFAULT_NAVN,
            List.of(new UngdomsprogramPeriode(nyProgramPeriode.getFomDato(), nyProgramPeriode.getTomDato())),
            satser,
            uttaksPerioder(nyProgramPeriode),
            tilkjentYtelsePerioder(satser, nyProgramPeriode),
            new LocalDateTimeline<>(fagsakPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(nyProgramPeriode, Utfall.OPPFYLT),
                new LocalDateSegment<>(nySluttdato.plusDays(1), fagsakPeriode.getTomDato(), Utfall.IKKE_OPPFYLT)
            )),
            fom.minusYears(19).plusDays(42),
            List.of(opprinneligProgramPeriode.getFomDato()),
            Set.of(
                new Trigger(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER, DatoIntervallEntitet.fra(opprinneligProgramPeriode.getFomDato(), nySluttdato)),
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM,
                    flyttetBakover ?
                        DatoIntervallEntitet.fra(nySluttdato.plusDays(1), opprinneligProgramPeriode.getTomDato()) :
                        DatoIntervallEntitet.fra(opprinneligProgramPeriode.getTomDato().plusDays(1), nySluttdato))
            ),
            null,
            Collections.emptyList(),
            null);
    }

    /**
     * Endrer startdato
     */
    public static UngTestScenario endringStartdato(LocalDate nyStartdato, LocalDateInterval opprinneligProgramPeriode) {

        if (nyStartdato.isEqual(opprinneligProgramPeriode.getFomDato())) {
            throw new IllegalArgumentException("Ny startdato er lik opprinnelig sluttdato");
        }

        boolean flyttetFremover = nyStartdato.isAfter(opprinneligProgramPeriode.getFomDato());
        LocalDate fom = flyttetFremover ? opprinneligProgramPeriode.getFomDato() : nyStartdato;

        var fagsakPeriode = new LocalDateInterval(fom,
            opprinneligProgramPeriode.getFomDato().plusWeeks(52).minusDays(1));

        var nyProgramPeriode = new LocalDateInterval(nyStartdato, opprinneligProgramPeriode.getTomDato());
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(nyStartdato, fagsakPeriode.getTomDato(), lavSatsBuilder(fom).build())
        ));

        var ungVilkårSegments = new ArrayList<LocalDateSegment<Utfall>>();
        ungVilkårSegments.add(new LocalDateSegment<>(nyStartdato, fagsakPeriode.getTomDato(), Utfall.OPPFYLT));

        if (flyttetFremover) {
            ungVilkårSegments.add(new LocalDateSegment<>(opprinneligProgramPeriode.getFomDato(), nyStartdato.minusDays(1), Utfall.IKKE_OPPFYLT));
        }

        return new UngTestScenario(
            DEFAULT_NAVN,
            List.of(new UngdomsprogramPeriode(nyProgramPeriode.getFomDato(), nyProgramPeriode.getTomDato())),
            satser,
            uttaksPerioder(fagsakPeriode),
            tilkjentYtelsePerioder(satser, fagsakPeriode),
            new LocalDateTimeline<>(fagsakPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(ungVilkårSegments),
            fom.minusYears(19).plusDays(42),
            List.of(opprinneligProgramPeriode.getFomDato()),
            Set.of(
                new Trigger(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER, DatoIntervallEntitet.fra(fom, fagsakPeriode.getTomDato())),
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_ENDRET_STARTDATO_UNGDOMSPROGRAM,
                    flyttetFremover ?
                        DatoIntervallEntitet.fra(opprinneligProgramPeriode.getFomDato(), nyStartdato.minusDays(1)) :
                        DatoIntervallEntitet.fra(fom, fagsakPeriode.getTomDato()))
            ),
            null,
            Collections.emptyList(),
            null);
    }

    /**
     * Opphør pga dødsfall i første måned
     */
    public static UngTestScenario død19år(LocalDate fom) {
        var dødsdato = fom.plusDays(15);
        var fagsakPeriode = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
        var nyProgramPeriode = new LocalDateInterval(fom, dødsdato.minusDays(1));
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fagsakPeriode.getFomDato(), fagsakPeriode.getTomDato(), lavSatsBuilder(fom).build())
        ));


        return new UngTestScenario(
            DEFAULT_NAVN,
            List.of(new UngdomsprogramPeriode(nyProgramPeriode.getFomDato(), nyProgramPeriode.getTomDato())),
            satser,
            uttaksPerioder(nyProgramPeriode),
            tilkjentYtelsePerioder(satser, nyProgramPeriode),
            new LocalDateTimeline<>(fagsakPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(nyProgramPeriode, Utfall.OPPFYLT),
                new LocalDateSegment<>(dødsdato, fagsakPeriode.getTomDato(), Utfall.IKKE_OPPFYLT)
            )),
            fom.minusYears(19).plusDays(42),
            List.of(fom),
            Set.of(
                new Trigger(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER, DatoIntervallEntitet.fra(fom, dødsdato.minusDays(1))),
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM, DatoIntervallEntitet.fra(dødsdato, fagsakPeriode.getTomDato()))
            ),
            null,
            Collections.emptyList(),
            dødsdato);
    }

    private static <T> LocalDateTimeline<T> splitPrMåned(LocalDateTimeline<T> satser) {
        return satser.splitAtRegular(satser.getMinLocalDate().withDayOfMonth(1), satser.getMaxLocalDate(), Period.ofMonths(1));
    }

    private static LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioder(LocalDateTimeline<UngdomsytelseSatser> satser, LocalDateInterval tilkjentPeriode) {
        return tilkjentYtelsePerioderMedReduksjon(satser, tilkjentPeriode, LocalDateTimeline.empty());
    }

    private static LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelsePerioderMedReduksjon(LocalDateTimeline<UngdomsytelseSatser> satsperioder, LocalDateInterval tilkjentPeriode, LocalDateTimeline<BigDecimal> rapportertInntektTimeline) {
        LocalDateTimeline<Boolean> ytelseTidslinje = splitPrMåned(new LocalDateTimeline<>(tilkjentPeriode, true));
        LocalDateTimeline<BeregnetSats> beregnetSats = TilkjentYtelseBeregner.mapSatserTilTotalbeløpForPerioder(satsperioder, ytelseTidslinje);
        return beregnetSats.intersection(ytelseTidslinje).combine(rapportertInntektTimeline,
            (s, lhs, rhs) -> {
                var rapportertInntekt = rhs == null ? BigDecimal.ZERO : rhs.getValue();
                return new LocalDateSegment<>(s, TilkjentYtelseBeregner.beregn(s, lhs.getValue(), rapportertInntekt).verdi());
            },
            LocalDateTimeline.JoinStyle.LEFT_JOIN
        );
    }


    @Test
    void testTilkjentYtelseReduksjonScenario() {
        var scenario = endringMedInntektPå10k_19år(LocalDate.of(2024, 12, 1));
        var andreMåned = scenario.tilkjentYtelsePerioder().getSegment(new LocalDateInterval(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)));

        assertThat(andreMåned.getFom()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(andreMåned.getTom()).isEqualTo(LocalDate.of(2025, 1, 31));

        //23 virkningsdager i januar 2025 med lav dagsats på 649,08. Rapportert inntekt er 10 000kr
        TilkjentYtelseVerdi t = andreMåned.getValue();
        assertThat(t.uredusertBeløp()).isEqualByComparingTo("14928.84"); //649,08 * 23
        assertThat(t.reduksjon()).isEqualByComparingTo("6600"); //66% av 10 0000
        assertThat(t.dagsats()).isEqualByComparingTo("362"); //649 - ((6600/22)  )
        assertThat(t.redusertBeløp()).isEqualByComparingTo("8328.84"); // 14928.84 - 6600
        assertThat(t.utbetalingsgrad()).isEqualTo(56); // 8328.84 / 14928.84 * 100

    }

    private static UngdomsytelseUttakPerioder uttaksPerioder(LocalDateInterval p) {
        UngdomsytelseUttakPerioder uttakperioder = new UngdomsytelseUttakPerioder(
            List.of(new UngdomsytelseUttakPeriode(
                DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato()))));
        uttakperioder.setRegelInput("regelInputUttak");
        uttakperioder.setRegelSporing("regelSporingUttak");
        return uttakperioder;
    }

    public static UngdomsytelseSatser.Builder lavSatsBuilder(LocalDate fom) {
        return lavSatsMedBarnBuilder(fom, 0);
    }

    private static BigDecimal hentGrunnbeløpFor(LocalDate fom) {
        return GrunnbeløpTidslinje.hentTidslinje().getSegment(new LocalDateInterval(fom, fom)).getValue().verdi();
    }

    public static UngdomsytelseSatser.Builder lavSatsMedBarnBuilder(LocalDate fom, int antallBarn) {
        SatsOgGrunnbeløpfaktor satsOgGrunnbeløpfaktor = hentSatstypeOgGrunnbeløp(Sats.LAV);
        var barneTillegg = BarnetilleggSatsTidslinje.BARNETILLEGG_DAGSATS.getSegment(new LocalDateInterval(fom, fom)).getValue();
        BigDecimal g = hentGrunnbeløpFor(fom);
        return UngdomsytelseSatser.builder()
            .medGrunnbeløp(g)
            .medGrunnbeløpFaktor(satsOgGrunnbeløpfaktor.grunnbeløpFaktor())
            .medSatstype(satsOgGrunnbeløpfaktor.satstype())
            .medAntallBarn(antallBarn)
            .medBarnetilleggDagsats(beregnDagsatsInklBarnetillegg(antallBarn, barneTillegg).intValue() );
    }

    public static UngdomsytelseSatser.Builder høySatsBuilder(LocalDate fom) {
        return høySatsBuilderMedBarn(fom, 0);
    }

    public static UngdomsytelseSatser.Builder høySatsBuilderMedBarn(LocalDate fom, int antallBarn) {
        SatsOgGrunnbeløpfaktor satsOgGrunnbeløpfaktor = hentSatstypeOgGrunnbeløp(Sats.HØY);
        var barneTillegg = BarnetilleggSatsTidslinje.BARNETILLEGG_DAGSATS.getSegment(new LocalDateInterval(fom, fom)).getValue();
        var g = hentGrunnbeløpFor(fom);
        return UngdomsytelseSatser.builder()
            .medGrunnbeløp(g)
            .medGrunnbeløpFaktor(satsOgGrunnbeløpfaktor.grunnbeløpFaktor())
            .medSatstype(satsOgGrunnbeløpfaktor.satstype())
            .medAntallBarn(antallBarn)
            .medBarnetilleggDagsats(beregnDagsatsInklBarnetillegg(antallBarn, barneTillegg).intValue() );
    }

    @NotNull
    private static BigDecimal beregnDagsatsInklBarnetillegg(int antallBarn, BigDecimal barneTillegg) {
        return barneTillegg.multiply(BigDecimal.valueOf(antallBarn));
    }

    private static SatsOgGrunnbeløpfaktor hentSatstypeOgGrunnbeløp(Sats sats) {
        return GrunnbeløpfaktorTidslinje.hentGrunnbeløpfaktorTidslinjeFor(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), sats)
        ))).stream().findFirst().orElseThrow().getValue();
    }
}
