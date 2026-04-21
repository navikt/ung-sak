package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPeriode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils.*;

public class AktivitetspengerEndringBarnetilleggScenarioer {

    /**
     * Fødselshendelse som gir barnetillegg. Bruker lav sats, får ett barn på fødselsdato.
     */
    public static AktivitetspengerTestScenario fødselMedEttBarn(LocalDate fom) {
        LocalDate fødselsdatoBarn = fom.plusDays(15);
        LocalDate fødselsdatoBruker = fom.minusYears(20);
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var satsUtenBarn = lavSatsBuilder(fom).build();
        var satsMedBarn = lavSatsMedBarnBuilder(fødselsdatoBarn, 1).build();

        var satsperioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, fødselsdatoBarn.minusDays(1), new AktivitetspengerSatsPeriode(new LocalDateInterval(fom, fødselsdatoBarn.minusDays(1)), satsUtenBarn)),
            new LocalDateSegment<>(fødselsdatoBarn, p.getTomDato(), new AktivitetspengerSatsPeriode(new LocalDateInterval(fødselsdatoBarn, p.getTomDato()), satsMedBarn))
        ));

        var satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, fødselsdatoBarn.minusDays(1), satsUtenBarn),
            new LocalDateSegment<>(fødselsdatoBarn, p.getTomDato(), satsMedBarn)
        ));

        var beregningsgrunnlag = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, null, lagBeregningsgrunnlag(fom))
        ));

        LocalDateInterval tilkjentPeriode = new LocalDateInterval(fom, p.getTomDato());

        return new AktivitetspengerTestScenario(
            DEFAULT_NAVN,
            List.of(new Periode(fom, p.getTomDato())),
            satsperioder,
            beregningsgrunnlag,
            tilkjentYtelsePerioder(satsGrunnlagTidslinje, tilkjentPeriode),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdatoBruker,
            Set.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DatoIntervallEntitet.fra(fødselsdatoBarn, p.getTomDato()))),
            Collections.emptyList(),
            null,
            null);
    }

    /**
     * Fødselshendelse som gir barnetillegg for to barn.
     */
    public static AktivitetspengerTestScenario fødselMedToBarn(LocalDate fom) {
        LocalDate fødselsdatoBarn = fom.plusDays(15);
        LocalDate fødselsdatoBruker = fom.minusYears(20);
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var satsUtenBarn = lavSatsBuilder(fom).build();
        var satsMedBarn = lavSatsMedBarnBuilder(fødselsdatoBarn, 2).build();

        var satsperioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, fødselsdatoBarn.minusDays(1), new AktivitetspengerSatsPeriode(new LocalDateInterval(fom, fødselsdatoBarn.minusDays(1)), satsUtenBarn)),
            new LocalDateSegment<>(fødselsdatoBarn, p.getTomDato(), new AktivitetspengerSatsPeriode(new LocalDateInterval(fødselsdatoBarn, p.getTomDato()), satsMedBarn))
        ));

        var satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, fødselsdatoBarn.minusDays(1), satsUtenBarn),
            new LocalDateSegment<>(fødselsdatoBarn, p.getTomDato(), satsMedBarn)
        ));

        var beregningsgrunnlag = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, null, lagBeregningsgrunnlag(fom))
        ));

        LocalDateInterval tilkjentPeriode = new LocalDateInterval(fom, p.getTomDato());

        return new AktivitetspengerTestScenario(
            DEFAULT_NAVN,
            List.of(new Periode(fom, p.getTomDato())),
            satsperioder,
            beregningsgrunnlag,
            tilkjentYtelsePerioder(satsGrunnlagTidslinje, tilkjentPeriode),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdatoBruker,
            Set.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DatoIntervallEntitet.fra(fødselsdatoBarn, p.getTomDato()))),
            Collections.emptyList(),
            null,
            null);
    }
}

