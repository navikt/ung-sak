package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.*;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPeriode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.InngangsvilkårVurderingTestData;
import no.nav.ung.ytelse.aktivitetspenger.testdata.VilkårUtfall;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils.*;

public class AktivitetspengerFørstegangsbehandlingScenarioer {

    private static final TreeSet<VilkårType> SORTERTE_VILKÅR = new TreeSet<>(
        List.of(
            VilkårType.ALDERSVILKÅR,
            VilkårType.BOSTEDSVILKÅR,
            VilkårType.BISTANDSVILKÅR,
            VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR)
    );

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
        return avslåttBostedScenario(fom, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, null);
    }

    public static AktivitetspengerTestScenario avslåttBistand(LocalDate fom, String fritekstBrev) {
        return avslåttBistandScenario(fom, BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK, fritekstBrev);
    }

    /**
     * Avslag pga bostedsvilkåret ikke oppfylt - folkeregistrert eller bostedsadresse.
     * Bruker er 20 år, men har verken bosted eller folkeregistrert adresse i Trondheim.
     */
    public static AktivitetspengerTestScenario avslåttBostedFolkeregistrertEllerBostedsadresse(LocalDate fom) {
        return avslåttBostedScenario(fom, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM, null);
    }

    public static AktivitetspengerTestScenario avslåttArbeidsstedStudiested(LocalDate fom, String fritekstBrev) {
        return avslåttBostedScenario(fom, BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM, fritekstBrev);
    }

    private static AktivitetspengerTestScenario avslåttBostedScenario(LocalDate fom, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstBrev) {
        var vurderinger = InngangsvilkårVurderingTestData.builder().medBostedsvilkårResultat(false, ikkeOppfyltÅrsak, fritekstBrev);
        return fullAvslagScenario(fom, VilkårType.BOSTEDSVILKÅR, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, fritekstBrev, vurderinger);
    }

    private static AktivitetspengerTestScenario avslåttBistandScenario(LocalDate fom, BistandsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstBrev) {
        var vurderinger = InngangsvilkårVurderingTestData.builder()
            .medBistandsvilkårResultat(false, ikkeOppfyltÅrsak, fritekstBrev);
        return fullAvslagScenario(fom, VilkårType.BISTANDSVILKÅR, Avslagsårsak.IKKE_14A_VEDTAK, fritekstBrev, vurderinger);
    }

    private static AktivitetspengerTestScenario fullAvslagScenario(LocalDate fom, VilkårType vilkårType, Avslagsårsak avslagsårsak, String fritekstBrev,
                                                                     InngangsvilkårVurderingTestData.Builder vurderinger) {
        LocalDate fødselsdato = fom.minusYears(20);
        var tom = fom.plusWeeks(52).minusDays(1);
        var p = new LocalDateInterval(fom, tom);
        var periode = new Periode(fom, tom);

        return AktivitetspengerTestScenario.builder()
            .medNavn(DEFAULT_NAVN)
            .medSøknadsperioder(List.of(periode))
            .medAldersvilkår(new LocalDateTimeline<>(p, Utfall.OPPFYLT))
            .medFødselsdato(fødselsdato)
            .medTriggere(Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fra(p))))
            .medVilkår(vilkårType, new LocalDateTimeline<>(p, VilkårUtfall.avslått(avslagsårsak, fritekstBrev)))
            .medInngangsvilkårVurderinger(vurderinger.medPeriode(periode).build())
            .build();
    }

    /**
     * Innvilget med tom-dato: bruker er over 25 år og får høy sats
     */
    public static AktivitetspengerTestScenario innvilgetMedAvkortetVilkår(LocalDate fom, LocalDate tom, VilkårType avkortetVilkårType) {
        return innvilgetMedAvslåttVilkår(fom, tom, avkortetVilkårType, Avslagsårsak.AVKORTET);
    }

    /**
     * Som {@link #innvilgetMedAvkortetVilkår}, men med valgfri avslagsårsak slik at delvis avslag
     * kan skilles fra avkorting.
     */
    public static AktivitetspengerTestScenario innvilgetMedAvslåttVilkår(LocalDate fom, LocalDate tom, VilkårType avslåttVilkår, Avslagsårsak avslagsårsak) {
        LocalDate maksTom = fom.plusWeeks(52).minusDays(1);
        if (!tom.isAfter(fom) || !tom.isBefore(maksTom)) {
            throw new IllegalArgumentException("tom må være etter fom og før maksimal sluttdato " + maksTom + ", var " + tom);
        }
        LocalDate fødselsdato = fom.minusYears(30);
        var fagsakPeriode = new LocalDateInterval(fom, maksTom);
        var innvilgetPeriode = new LocalDateInterval(fom, tom);

        var høySats = høySatsBuilder(fom).build();

        var satsperioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(innvilgetPeriode, new AktivitetspengerSatsPeriode(innvilgetPeriode, høySats))
        ));

        var satsGrunnlagTidslinje = new LocalDateTimeline<>(innvilgetPeriode, høySats);

        var beregningsgrunnlag = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, null, lagBeregningsgrunnlag(fom))
        ));

        // Tilkjent ytelse beregnes ut inneværende måned, men aldri lenger enn den avkortede sluttdatoen
        LocalDate a = fom.with(TemporalAdjusters.lastDayOfMonth());
        LocalDate tilkjentTom = a.isBefore(tom) ? a : tom;
        LocalDateInterval tilkjentPeriode = new LocalDateInterval(fom, tilkjentTom);

        var avkortetTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(innvilgetPeriode, VilkårUtfall.oppfylt()),
            new LocalDateSegment<>(tom.plusDays(1), maksTom, VilkårUtfall.avslått(avslagsårsak))
        ));

        var builder = AktivitetspengerTestScenario.builder()
            .medNavn(DEFAULT_NAVN)
            .medSøknadsperioder(List.of(new Periode(fagsakPeriode.getFomDato(), fagsakPeriode.getTomDato())))
            .medSatsperioder(satsperioder)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medTilkjentYtelse(tilkjentYtelsePerioder(lagSatserTidslinje(satsGrunnlagTidslinje, beregningsgrunnlag), tilkjentPeriode))
            .medFødselsdato(fødselsdato)
            .medTriggere(Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PERIODE, DatoIntervallEntitet.fra(fagsakPeriode))));

        // TODO: Flytt aldersvilkår til medVilkår
        if (avslåttVilkår == VilkårType.ALDERSVILKÅR) {
            builder.medAldersvilkår(avkortetTidslinje.mapValue(v -> Objects.equals(v, VilkårUtfall.oppfylt()) ? Utfall.OPPFYLT : Utfall.IKKE_OPPFYLT));
        } else {
            builder.medVilkår(avslåttVilkår, avkortetTidslinje);
        }




        //Oppfyll vilkårene før
        SORTERTE_VILKÅR.headSet(avslåttVilkår, false)
            .stream().filter(it -> it != VilkårType.ALDERSVILKÅR)
            .forEach(vilkårType -> builder.medVilkår(vilkårType,
                new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fagsakPeriode, VilkårUtfall.oppfylt())))));

        //Sett til ikke relevant vilkårene etter
        SORTERTE_VILKÅR.tailSet(avslåttVilkår, false)
            .stream().filter(it -> it != VilkårType.ALDERSVILKÅR)
            .forEach(vilkårType ->
                builder.medVilkår(vilkårType,
                    new LocalDateTimeline<>(List.of(
                        new LocalDateSegment<>(innvilgetPeriode, VilkårUtfall.oppfylt()),
                        new LocalDateSegment<>(tom.plusDays(1), maksTom, VilkårUtfall.ikkeRelevant()))
                    )));

        return builder.build();
    }

}

