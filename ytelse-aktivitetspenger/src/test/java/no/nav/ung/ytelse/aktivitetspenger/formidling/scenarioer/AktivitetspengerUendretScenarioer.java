package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPeriode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils.*;

/**
 * Scenarioer der et inngangsvilkår er avklart av saksbehandler (varslet opphør/avslag), men vurderingen konkluderer
 * med at vilkåret fortsatt er oppfylt. Gir grunnlag for et "uendret vedtak"-brev.
 */
public class AktivitetspengerUendretScenarioer {

    public static AktivitetspengerTestScenario uendretScenario(LocalDate fom, BostedsAvklaringTestData bostedsAvklaring, String fritekstTilBrev) {
        return uendretScenarioBuilder(fom, bostedsAvklaring, fritekstTilBrev).build();
    }

    public static AktivitetspengerTestScenario.Builder uendretScenarioBuilder(LocalDate fom, BostedsAvklaringTestData bostedsAvklaring, String fritekstTilBrev) {
        var vurdertPeriode = bostedsAvklaring.periode();
        var inngangsvilkårVurderinger = InngangsvilkårVurderingTestData.builder()
            .medBostedsvilkårResultat(vurdertPeriode, true, null, fritekstTilBrev)
            .build();

        return uendretBuilder(fom, BehandlingÅrsakType.ENDRET_BOSTED, maksperiode(fom))
            .medInngangsvilkårVurderinger(inngangsvilkårVurderinger)
            .medVilkår(VilkårType.BOSTEDSVILKÅR, new LocalDateTimeline<>(vurdertPeriode.getFom(), vurdertPeriode.getTom(), VilkårUtfall.oppfylt()))
            .medBostedsAvklaringer(List.of(bostedsAvklaring));
    }

    public static AktivitetspengerTestScenario uendretLivsoppholdScenario(LocalDate fom, VilkårsavklaringTestData avklaring, String fritekstTilBrev) {
        var vurdertPeriode = avklaring.periode();
        var inngangsvilkårVurderinger = InngangsvilkårVurderingTestData.builder()
            .medAndreYtelser(vurdertPeriode, true, null, fritekstTilBrev)
            .build();

        return uendretBuilder(fom, BehandlingÅrsakType.ENDRET_LIVSOPPHOLDSYTELSE, maksperiode(fom))
            .medInngangsvilkårVurderinger(inngangsvilkårVurderinger)
            .medVilkår(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, new LocalDateTimeline<>(vurdertPeriode.getFom(), vurdertPeriode.getTom(), VilkårUtfall.oppfylt()))
            .medVilkårsavklaringer(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, List.of(avklaring))
            .build();
    }

    public static AktivitetspengerTestScenario uendretBistandScenario(LocalDate fom, VilkårsavklaringTestData avklaring, String fritekstTilBrev) {
        var vurdertPeriode = avklaring.periode();
        var inngangsvilkårVurderinger = InngangsvilkårVurderingTestData.builder()
            .medBistandsvilkårResultat(vurdertPeriode, true, null, fritekstTilBrev)
            .build();

        return uendretBuilder(fom, BehandlingÅrsakType.ENDRET_BISTANDSBEHOV, maksperiode(fom))
            .medInngangsvilkårVurderinger(inngangsvilkårVurderinger)
            .medVilkår(VilkårType.BISTANDSVILKÅR, new LocalDateTimeline<>(vurdertPeriode.getFom(), vurdertPeriode.getTom(), VilkårUtfall.oppfylt()))
            .medVilkårsavklaringer(VilkårType.BISTANDSVILKÅR, List.of(avklaring))
            .build();
    }

    /**
     * Bistandsvilkåret er fortsatt oppfylt, men avklaringen dekker også en hale som er avkortet fra avkortetFom.
     */
    public static AktivitetspengerTestScenario uendretBistandMedAvkortetHale(LocalDate fom, VilkårsavklaringTestData avklaring, LocalDate avkortetFom) {
        var vurdertPeriode = avklaring.periode();
        var inngangsvilkårVurderinger = InngangsvilkårVurderingTestData.builder()
            .medBistandsvilkårResultat(new Periode(vurdertPeriode.getFom(), avkortetFom.minusDays(1)), true, null, null)
            .medBistandsvilkårResultat(new Periode(avkortetFom, vurdertPeriode.getTom()), false, BistandsvilkårIkkeOppfyltÅrsak.AVKORTET, null)
            .build();

        var vilkårTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(vurdertPeriode.getFom(), avkortetFom.minusDays(1), VilkårUtfall.oppfylt()),
            new LocalDateSegment<>(avkortetFom, vurdertPeriode.getTom(), VilkårUtfall.avslått(Avslagsårsak.AVKORTET))
        ));

        return uendretBuilder(fom, BehandlingÅrsakType.ENDRET_BISTANDSBEHOV, new LocalDateInterval(fom, avkortetFom.minusDays(1)))
            .medInngangsvilkårVurderinger(inngangsvilkårVurderinger)
            .medVilkår(VilkårType.BISTANDSVILKÅR, vilkårTidslinje)
            .medVilkårsavklaringer(VilkårType.BISTANDSVILKÅR, List.of(avklaring))
            .build();
    }

    private static LocalDateInterval maksperiode(LocalDate fom) {
        return new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
    }

    private static AktivitetspengerTestScenario.Builder uendretBuilder(LocalDate fom, BehandlingÅrsakType årsak, LocalDateInterval tilkjentPeriode) {
        var p = maksperiode(fom);
        var lavSats = lavSatsBuilder(fom).build();
        var satsperioder = new LocalDateTimeline<>(p, new AktivitetspengerSatsPeriode(p, lavSats));
        var satsGrunnlagTidslinje = new LocalDateTimeline<>(p, lavSats);
        var beregningsgrunnlag = new LocalDateTimeline<>(fom, null, lagBeregningsgrunnlag(fom));

        return AktivitetspengerTestScenario.builder()
            .medNavn(DEFAULT_NAVN)
            .medSøknadsperioder(List.of(new Periode(p.getFomDato(), p.getTomDato())))
            .medSatsperioder(satsperioder)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medTilkjentYtelse(tilkjentYtelsePerioder(lagSatserTidslinje(satsGrunnlagTidslinje, beregningsgrunnlag), tilkjentPeriode))
            .medVilkår(VilkårType.ALDERSVILKÅR, new LocalDateTimeline<>(p, VilkårUtfall.oppfylt()))
            .medFødselsdato(fom.minusYears(20))
            .medTriggere(Set.of(new Trigger(årsak, DatoIntervallEntitet.fra(p))));
    }
}
