package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestScenario;
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
     * Kombinasjon - forlenget periode + kontroll av inntekt (full utbetaling, dvs. 0 kr inntekt).
     * Skal gi forlenget-brev men ingen inntektskontroll-brev.
     *
     * @param fom                  - startdato for programmet
     * @param opprinneligSluttdato - opprinnelig sluttdato (slik det var i forrige behandling)
     * @param nySluttdato          - ny sluttdato etter forlengelse
     */
    public static UngTestScenario kombinasjon_forlengetPeriodeOgKontrollInntektFullUtbetaling(LocalDate fom, LocalDate opprinneligSluttdato, LocalDate nySluttdato) {
        if (!nySluttdato.isAfter(opprinneligSluttdato)) {
            throw new IllegalArgumentException("Ny sluttdato må være etter opprinnelig sluttdato");
        }

        var nyProgramPeriode = new LocalDateInterval(fom, nySluttdato);
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, nySluttdato, BrevScenarioerUtils.lavSatsBuilder(fom).build())
        ));

        // Kontroll av inntekt for andre måned (0 kr → full utbetaling)
        var kontrollPeriode = new LocalDateInterval(
            fom.withDayOfMonth(1).plusMonths(1),
            fom.withDayOfMonth(1).plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()));

        var kontrollertInntektTimeline = BrevScenarioerUtils.splitPrMåned(new LocalDateTimeline<>(kontrollPeriode,
            BrevScenarioerUtils.KontrollerInntektHolder.forRegisterInntekt(BigDecimal.ZERO)));

        var kontrollerInntektPerioder = BrevScenarioerUtils.kontrollerInntektFraHolder(nyProgramPeriode, kontrollertInntektTimeline);

        var satserPrMåned = BrevScenarioerUtils.splitPrMåned(satser);
        var tilkjentYtelsePerioder = BrevScenarioerUtils.tilkjentYtelsePerioderMedReduksjon(satserPrMåned, kontrollPeriode, kontrollertInntektTimeline);

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new UngdomsprogramPeriode(fom, nySluttdato)),
            satser,
            BrevScenarioerUtils.uttaksPerioder(nyProgramPeriode),
            tilkjentYtelsePerioder,
            new LocalDateTimeline<>(nyProgramPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(nyProgramPeriode, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(fom),
            Set.of(
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM,
                    DatoIntervallEntitet.fra(opprinneligSluttdato.plusDays(1), nySluttdato)),
                new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, DatoIntervallEntitet.fra(kontrollPeriode)),
                new Trigger(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, DatoIntervallEntitet.fra(kontrollPeriode))
            ),
            Collections.emptyList(),
            null,
            kontrollerInntektPerioder, null);
    }


    /**
     * Kombinasjon - forlenget periode + kontroll av inntekt med reduksjon (10 000 kr inntekt).
     * Skal gi to brev: ENDRING_INNTEKT og FORLENGET_PERIODE.
     *
     * @param fom                  - startdato for programmet
     * @param opprinneligSluttdato - opprinnelig sluttdato (slik det var i forrige behandling)
     * @param nySluttdato          - ny sluttdato etter forlengelse
     */
    public static UngTestScenario kombinasjon_forlengetPeriodeOgKontrollInntektMedReduksjon(LocalDate fom, LocalDate opprinneligSluttdato, LocalDate nySluttdato) {
        if (!nySluttdato.isAfter(opprinneligSluttdato)) {
            throw new IllegalArgumentException("Ny sluttdato må være etter opprinnelig sluttdato");
        }

        var nyProgramPeriode = new LocalDateInterval(fom, nySluttdato);
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, nySluttdato, BrevScenarioerUtils.lavSatsBuilder(fom).build())
        ));

        // Kontroll av inntekt for andre måned (10 000 kr → reduksjon)
        var kontrollPeriode = new LocalDateInterval(
            fom.withDayOfMonth(1).plusMonths(1),
            fom.withDayOfMonth(1).plusMonths(1).with(TemporalAdjusters.lastDayOfMonth()));

        var kontrollertInntektTimeline = BrevScenarioerUtils.splitPrMåned(new LocalDateTimeline<>(kontrollPeriode,
            BrevScenarioerUtils.KontrollerInntektHolder.forRegisterInntekt(BigDecimal.valueOf(10000))));

        var kontrollerInntektPerioder = BrevScenarioerUtils.kontrollerInntektFraHolder(nyProgramPeriode, kontrollertInntektTimeline);

        var satserPrMåned = BrevScenarioerUtils.splitPrMåned(satser);
        var tilkjentYtelsePerioder = BrevScenarioerUtils.tilkjentYtelsePerioderMedReduksjon(satserPrMåned, kontrollPeriode, kontrollertInntektTimeline);

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new UngdomsprogramPeriode(fom, nySluttdato)),
            satser,
            BrevScenarioerUtils.uttaksPerioder(nyProgramPeriode),
            tilkjentYtelsePerioder,
            new LocalDateTimeline<>(nyProgramPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(nyProgramPeriode, Utfall.OPPFYLT),
            fom.minusYears(19).plusDays(42),
            List.of(fom),
            Set.of(
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM,
                    DatoIntervallEntitet.fra(opprinneligSluttdato.plusDays(1), nySluttdato)),
                new Trigger(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, DatoIntervallEntitet.fra(kontrollPeriode)),
                new Trigger(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT, DatoIntervallEntitet.fra(kontrollPeriode))
            ),
            Collections.emptyList(),
            null,
            kontrollerInntektPerioder, null);
    }


    /**
     * Kombinasjon - forlenget periode + opphør i samme behandling.
     * Perioden er først forlenget fra opprinnelig sluttdato til ny sluttdato, men programmet
     * opphører deretter ved opphørsdato (som ligger i den forlengede delen av perioden).
     * Skal gi både forlenget-trigger og opphørs-trigger.
     *
     * @param fom                  - startdato for programmet
     * @param opphørsdato          - dato programmet opphører (siste dag med ytelse)
     */
    public static UngTestScenario kombinasjon_forlengetPeriodeOgOpphør(LocalDate fom, LocalDate opphørsdato) {
        // Opprinnelig sluttdato er 52 uker fra fom, ny sluttdato er 8 uker etter opprinnelig sluttdato
        var opprinneligSluttdato = fom.plusWeeks(52).minusDays(1);
        var nySluttdato = opprinneligSluttdato.plusWeeks(8);
        if (!opphørsdato.isAfter(opprinneligSluttdato) || !opphørsdato.isBefore(nySluttdato)) {
            throw new IllegalArgumentException("Opphørsdato må ligge i den forlengede delen av perioden (etter opprinnelig sluttdato og før ny sluttdato)");
        }

        var forlengetPeriode = new LocalDateInterval(fom, nySluttdato);
        var nyProgramPeriode = new LocalDateInterval(fom, opphørsdato);
        var satser = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, nySluttdato, BrevScenarioerUtils.lavSatsBuilder(fom).build())
        ));

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new UngdomsprogramPeriode(nyProgramPeriode.getFomDato(), nyProgramPeriode.getTomDato())),
            satser,
            BrevScenarioerUtils.uttaksPerioder(nyProgramPeriode),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, nyProgramPeriode),
            new LocalDateTimeline<>(forlengetPeriode, Utfall.OPPFYLT),
            new LocalDateTimeline<>(List.of(
                new LocalDateSegment<>(nyProgramPeriode, Utfall.OPPFYLT),
                new LocalDateSegment<>(opphørsdato.plusDays(1), nySluttdato, Utfall.IKKE_OPPFYLT)
            )),
            fom.minusYears(19).plusDays(42),
            List.of(fom),
            Set.of(
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM,
                    DatoIntervallEntitet.fra(opprinneligSluttdato.plusDays(1), nySluttdato)),
                new Trigger(BehandlingÅrsakType.UTTALELSE_FRA_BRUKER,
                    DatoIntervallEntitet.fra(fom, opphørsdato)),
                new Trigger(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM,
                    DatoIntervallEntitet.fra(opphørsdato.plusDays(1), nySluttdato))
            ),
            Collections.emptyList(),
            null, null, null);
    }


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
        var rapportertInntektTimeline = BrevScenarioerUtils.splitPrMåned(new LocalDateTimeline<>(rapportertInntektPeriode,
            BrevScenarioerUtils.KontrollerInntektHolder.forRegisterInntekt(BigDecimal.valueOf(10000))));
        var tilkjentYtelsePerioder = BrevScenarioerUtils.tilkjentYtelsePerioderMedReduksjon(satserPrMåned, rapportertInntektPeriode, rapportertInntektTimeline);

        var kontrollerInntektPerioder = BrevScenarioerUtils.kontrollerInntektFraHolder(p, rapportertInntektTimeline);


        var opptjening = OppgittOpptjeningBuilder.ny();

        rapportertInntektTimeline.forEach(it ->
            opptjening.leggTilOppgittArbeidsforhold(OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
                .medInntekt(it.getValue().rapportertInntekt())
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
            ), null,
            kontrollerInntektPerioder, null);
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
        var rapportertInntektTimeline = BrevScenarioerUtils.splitPrMåned(new LocalDateTimeline<>(rapportertInntektPeriode,
            BrevScenarioerUtils.KontrollerInntektHolder.forRegisterInntekt(BigDecimal.valueOf(10000))));
        var tilkjentYtelsePerioder = BrevScenarioerUtils.tilkjentYtelsePerioderMedReduksjon(satserPrMåned, rapportertInntektPeriode, rapportertInntektTimeline);

        var kontrollerInntektPerioder = BrevScenarioerUtils.kontrollerInntektFraHolder(p, rapportertInntektTimeline);

        var opptjening = OppgittOpptjeningBuilder.ny();

        rapportertInntektTimeline.forEach(it ->
            opptjening.leggTilOppgittArbeidsforhold(OppgittOpptjeningBuilder.OppgittArbeidsforholdBuilder.ny()
                .medInntekt(it.getValue().rapportertInntekt())
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
            null,
            kontrollerInntektPerioder, null);
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
            null, null, null);
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
            null, null, null);
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
            null, null, null);
    }

    public static UngTestScenario leggTilVarselOpphørVedMaksdato(UngTestScenario scenario, LocalDate maksdato) {
        var triggere = new HashSet<>(scenario.behandlingTriggere());
        triggere.add(new Trigger(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO, DatoIntervallEntitet.fraOgMedTilOgMed(maksdato, maksdato)));
        return new UngTestScenario(
            scenario.navn(),
            scenario.programPerioder(),
            scenario.satser(),
            scenario.uttakPerioder(),
            scenario.tilkjentYtelsePerioder(),
            scenario.aldersvilkår(),
            scenario.ungdomsprogramvilkår(),
            scenario.fødselsdato(),
            scenario.søknadStartDato(),
            triggere,
            scenario.barn(),
            scenario.dødsdato(),
            scenario.kontrollerInntektPerioder(),
            maksdato);
    }

}
