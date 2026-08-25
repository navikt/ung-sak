package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPeriode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.VilkårUtfall;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils.*;

public class AktivitetspengerFørstegangsbehandlingScenarioer {

    /**
     * 24 år, blir 25 år etter 15 dager i programmet.
     * Lav sats med besteberegning (SISTE_ÅR, 300 000 kr) som er høyere enn lav minstesats.
     * Høy sats med minstesats etter 25-årsdagen.
     * Ingen inntektsgradering og ingen barn.
     */
    public static AktivitetspengerTestScenario innvilget24ÅrBle25ÅrLavSatsMedBesteberegning(LocalDate fom) {
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
            new LocalDateSegment<>(fom, null, lagBeregningsgrunnlagHøyereEnnLavMinstesats(fom))
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
     * Person over 25 år gjennom hele programperioden.
     * Kun høy sats (minstesats). Ingen inntektsgradering og ingen barn.
     */
    public static AktivitetspengerTestScenario innvilgetKunHøySats(LocalDate fom) {
        LocalDate fødselsdato = fom.minusYears(30);
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var høySats = høySatsBuilder(fom).build();

        var satsperioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, p.getTomDato(), new AktivitetspengerSatsPeriode(new LocalDateInterval(fom, p.getTomDato()), høySats))
        ));

        var satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, p.getTomDato(), høySats)
        ));

        var beregningsgrunnlag = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, null, lagBeregningsgrunnlag(fom))
        ));

        LocalDateInterval tilkjentPeriode = new LocalDateInterval(fom, fom.with(TemporalAdjusters.lastDayOfMonth()));

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
     * Person under 25 år gjennom hele programperioden.
     * Kun lav sats (minstesats). Ingen inntektsgradering og ingen barn.
     */
    public static AktivitetspengerTestScenario innvilgetKunLavSats(LocalDate fom) {
        LocalDate fødselsdato = fom.minusYears(20);
        var p = new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));

        var lavSats = lavSatsBuilder(fom).build();

        var satsperioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, p.getTomDato(), new AktivitetspengerSatsPeriode(new LocalDateInterval(fom, p.getTomDato()), lavSats))
        ));

        var satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, p.getTomDato(), lavSats)
        ));

        var beregningsgrunnlag = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, null, lagBeregningsgrunnlag(fom))
        ));

        LocalDateInterval tilkjentPeriode = new LocalDateInterval(fom, fom.with(TemporalAdjusters.lastDayOfMonth()));

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
    public static AktivitetspengerTestScenario avslåttBosted(LocalDate fom) {
        return fullAvslagScenario(fom, VilkårType.BOSTEDSVILKÅR, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, null);
    }

    public static AktivitetspengerTestScenario avslåttBistand(LocalDate fom, String fritekstBrev) {
        return fullAvslagScenario(fom, VilkårType.BISTANDSVILKÅR, Avslagsårsak.IKKE_14A_VEDTAK, fritekstBrev);
    }

    /**
     * Avslag pga bostedsvilkåret ikke oppfylt - folkeregistrert eller bostedsadresse.
     * Bruker er 20 år, men har verken bosted eller folkeregistrert adresse i Trondheim.
     */
    public static AktivitetspengerTestScenario avslåttBostedFolkeregistrertEllerBostedsadresse(LocalDate fom) {
        return fullAvslagScenario(fom, VilkårType.BOSTEDSVILKÅR, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_FOLKEREGISTRERT_ELLER_BOSTEDSADRESSE, null);
    }

    public static AktivitetspengerTestScenario avslåttArbeidsstedStudiested(LocalDate fom, String fritekstBrev) {
        return fullAvslagScenario(fom, VilkårType.BOSTEDSVILKÅR, Avslagsårsak.YTELSE_IKKE_PÅ_ARBEIDSSTED_STUDIESTED, fritekstBrev);
    }

    private static AktivitetspengerTestScenario fullAvslagScenario(LocalDate fom, VilkårType vilkårType, Avslagsårsak avslagsårsak, String fritekstBrev) {
        LocalDate fødselsdato = fom.minusYears(20);
        var tom = fom.plusWeeks(52).minusDays(1);
        var p = new LocalDateInterval(fom, tom);

        return AktivitetspengerTestScenario.builder()
            .medNavn(DEFAULT_NAVN)
            .medSøknadsperioder(List.of(new Periode(fom, tom)))
            .medAldersvilkår(new LocalDateTimeline<>(p, Utfall.OPPFYLT))
            .medFødselsdato(fødselsdato)
            .medTriggere(Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fra(p))))
            .medVilkår(vilkårType, new LocalDateTimeline<>(p, VilkårUtfall.avslått(avslagsårsak, fritekstBrev)))
            .build();
    }

}

