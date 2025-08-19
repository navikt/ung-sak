package no.nav.ung.sak.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import no.nav.ung.sak.trigger.Trigger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EndringProgramPeriodeScenarioer {
    /**
     *
     *  Opphør av programmet.
     *
     * @param opprinneligProgramPeriode - den perioden som opprinnelig ble innvilget
     * @param sluttdato - sluttdato
     */
    public static UngTestScenario endringOpphør(LocalDateInterval opprinneligProgramPeriode, LocalDate sluttdato) {
        var fom = opprinneligProgramPeriode.getFomDato();
        var fagsakPeriode = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
        var nyProgramPeriode = new LocalDateInterval(fom, sluttdato);
        var satser = new LocalDateTimeline<>(List.of(
           new LocalDateSegment<>(fagsakPeriode.getFomDato(), fagsakPeriode.getTomDato(), BrevScenarioerUtils.lavSatsBuilder(fom).build())
        ));


        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
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
            new LocalDateSegment<>(opprinneligProgramPeriode.getFomDato(), opprinneligProgramPeriode.getTomDato(), BrevScenarioerUtils.lavSatsBuilder(fom).build())
        ));

        boolean flyttetBakover = nySluttdato.isBefore(opprinneligProgramPeriode.getTomDato());

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new UngdomsprogramPeriode(nyProgramPeriode.getFomDato(), nyProgramPeriode.getTomDato())),
            satser,
            BrevScenarioerUtils.uttaksPerioder(nyProgramPeriode),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, nyProgramPeriode),
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
            new LocalDateSegment<>(nyStartdato, fagsakPeriode.getTomDato(), BrevScenarioerUtils.lavSatsBuilder(fom).build())
        ));

        var ungVilkårSegments = new ArrayList<LocalDateSegment<Utfall>>();
        ungVilkårSegments.add(new LocalDateSegment<>(nyStartdato, fagsakPeriode.getTomDato(), Utfall.OPPFYLT));

        if (flyttetFremover) {
            ungVilkårSegments.add(new LocalDateSegment<>(opprinneligProgramPeriode.getFomDato(), nyStartdato.minusDays(1), Utfall.IKKE_OPPFYLT));
        }

        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new UngdomsprogramPeriode(nyProgramPeriode.getFomDato(), nyProgramPeriode.getTomDato())),
            satser,
            BrevScenarioerUtils.uttaksPerioder(fagsakPeriode),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, fagsakPeriode),
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
            new LocalDateSegment<>(fagsakPeriode.getFomDato(), fagsakPeriode.getTomDato(), BrevScenarioerUtils.lavSatsBuilder(fom).build())
        ));


        return new UngTestScenario(
            BrevScenarioerUtils.DEFAULT_NAVN,
            List.of(new UngdomsprogramPeriode(nyProgramPeriode.getFomDato(), nyProgramPeriode.getTomDato())),
            satser,
            BrevScenarioerUtils.uttaksPerioder(nyProgramPeriode),
            BrevScenarioerUtils.tilkjentYtelsePerioder(satser, nyProgramPeriode),
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
}
