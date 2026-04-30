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
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils.*;

public class AktivitetspengerFørstegangsbehandlingScenarioer {

    /**
     * 24 år, blir 25 år etter 15 dager i programmet.
     * Får både lav og høy sats i førstegangsbehandlingen.
     * Ingen inntektsgradering og ingen barn.
     */
    public static AktivitetspengerTestScenario innvilget24årBle25årførsteMåned(LocalDate fom) {
        LocalDate tjuvefemårsdag = fom.plusDays(15);
        LocalDate fødselsdato = tjuvefemårsdag.minusYears(25);
        LocalDate tom25årmnd = tjuvefemårsdag.with(TemporalAdjusters.lastDayOfMonth());
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var lavSats = lavSatsBuilder(p.getFomDato()).build();
        var høySats = høySatsBuilder(tjuvefemårsdag).build();

        var satsperioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tjuvefemårsdag.minusDays(1), new AktivitetspengerSatsPeriode(new LocalDateInterval(fom, tjuvefemårsdag.minusDays(1)), lavSats)),
            new LocalDateSegment<>(tjuvefemårsdag, p.getTomDato(), new AktivitetspengerSatsPeriode(new LocalDateInterval(tjuvefemårsdag, p.getTomDato()), høySats))
        ));

        var satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tjuvefemårsdag.minusDays(1), lavSats),
            new LocalDateSegment<>(tjuvefemårsdag, p.getTomDato(), høySats)
        ));

        var beregningsgrunnlag = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, null, lagBeregningsgrunnlag(fom))
        ));

        LocalDateInterval tilkjentPeriode = new LocalDateInterval(fom, tom25årmnd);

        return new AktivitetspengerTestScenario(
            DEFAULT_NAVN,
            List.of(new Periode(fom, p.getTomDato())),
            satsperioder,
            beregningsgrunnlag,
            tilkjentYtelsePerioder(lagSatserTidslinje(satsGrunnlagTidslinje, beregningsgrunnlag), tilkjentPeriode),
            new LocalDateTimeline<>(p, Utfall.OPPFYLT),
            fødselsdato,
            Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fra(p))),
            Collections.emptyList(),
            null,
            null);
    }

    /**
     * Avslag pga bostedsvilkåret ikke oppfylt.
     * Bruker er 20 år, har søkt fra fom, men bor utenfor EØS.
     */
    public static AvslagScenario avslåttBosted(LocalDate fom) {
        LocalDate fødselsdato = fom.minusYears(20);
        var tom = fom.plusWeeks(52).minusDays(1);
        var p = new LocalDateInterval(fom, tom);

        return new AvslagScenario(
            new AktivitetspengerTestScenario(
                DEFAULT_NAVN,
                List.of(new Periode(fom, tom)),
                null,
                null,
                null,
                new LocalDateTimeline<>(p, Utfall.OPPFYLT),
                fødselsdato,
                Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fra(p))),
                Collections.emptyList(),
                null,
                null),
            new Periode(fom, tom),
            no.nav.ung.kodeverk.vilkår.VilkårType.BOSTEDSVILKÅR,
            no.nav.ung.kodeverk.vilkår.Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED
        );
    }

    /**
     * Avslag pga bistandsvilkåret ikke oppfylt.
     * Bruker er 20 år, har søkt fra fom, men mangler 14a-vedtak.
     */
    public static AvslagScenario avslåttBistand(LocalDate fom) {
        LocalDate fødselsdato = fom.minusYears(20);
        var tom = fom.plusWeeks(52).minusDays(1);
        var p = new LocalDateInterval(fom, tom);

        return new AvslagScenario(
            new AktivitetspengerTestScenario(
                DEFAULT_NAVN,
                List.of(new Periode(fom, tom)),
                null,
                null,
                null,
                new LocalDateTimeline<>(p, Utfall.OPPFYLT),
                fødselsdato,
                Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fra(p))),
                Collections.emptyList(),
                null,
                null),
            new Periode(fom, tom),
            no.nav.ung.kodeverk.vilkår.VilkårType.BISTANDSVILKÅR,
            no.nav.ung.kodeverk.vilkår.Avslagsårsak.IKKE_14A_VEDTAK
        );
    }

    public record AvslagScenario(
        AktivitetspengerTestScenario testScenario,
        Periode vilkårPeriode,
        no.nav.ung.kodeverk.vilkår.VilkårType vilkårType,
        no.nav.ung.kodeverk.vilkår.Avslagsårsak avslagsårsak
    ) {}
}

