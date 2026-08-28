package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.Trigger;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsPeriode;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.BostedsAvklaringTestData;
import no.nav.ung.ytelse.aktivitetspenger.testdata.InngangsvilkårVurderingTestData;
import no.nav.ung.ytelse.aktivitetspenger.testdata.VilkårUtfall;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils.*;

/**
 * Scenarioer der bostedsvilkåret er avslått av saksbehandler for en avgrenset periode midt i programperioden
 * (i motsetning til opphør, som varer ut programperioden). Gir grunnlag for et "endring_avslag"-brev.
 */
public class AktivitetspengerEndringAvslagScenarioer {

    public static AktivitetspengerTestScenario avslagPgaBosted(LocalDate fom) {
        return avslagMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, null);
    }

    public static AktivitetspengerTestScenario avslagPgaBostedAnnet(LocalDate fom, String fritekstTilBrev) {
        return avslagMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.ANNET, fritekstTilBrev);
    }

    public static AktivitetspengerTestScenario avslagPgaBostedFolkeregistrert(LocalDate fom) {
        return avslagMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM, null);
    }

    public static AktivitetspengerTestScenario avslagPgaArbeidsstedStudiested(LocalDate fom) {
        return avslagMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM, null);
    }

    private static AktivitetspengerTestScenario avslagMedÅrsak(LocalDate fom, VilkårType vilkårType, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev) {
        LocalDate fødselsdato = fom.minusYears(20);
        var tom = fom.plusWeeks(52).minusDays(1);
        var p = new LocalDateInterval(fom, tom);

        var avslagFom = fom.plusMonths(3);
        var avslagTom = fom.plusMonths(5).minusDays(1);
        var avslåttVilkårPeriode = new Periode(avslagFom, avslagTom);

        var lavSats = lavSatsBuilder(fom).build();
        var satsperioder = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tom, new AktivitetspengerSatsPeriode(p, lavSats))
        ));

        var satsGrunnlagTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, tom, lavSats)
        ));

        var beregningsgrunnlag = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fom, null, lagBeregningsgrunnlag(fom))
        ));

        var satserTidslinje = lagSatserTidslinje(satsGrunnlagTidslinje, beregningsgrunnlag);
        var tilkjentYtelse = tilkjentYtelsePerioder(satserTidslinje, new LocalDateInterval(fom, avslagFom.minusDays(1)))
            .crossJoin(tilkjentYtelsePerioder(satserTidslinje, new LocalDateInterval(avslagTom.plusDays(1), tom)));

        var inngangsvilkårVurderinger = InngangsvilkårVurderingTestData.builder()
            .medBostedsvilkårResultat(avslåttVilkårPeriode, false, ikkeOppfyltÅrsak, fritekstTilBrev)
            .build();

        var bostedVilkårTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(avslåttVilkårPeriode.getFom(), avslåttVilkårPeriode.getTom(),
                VilkårUtfall.avslått(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, fritekstTilBrev))
        ));

        return AktivitetspengerTestScenario.builder()
            .medNavn(DEFAULT_NAVN)
            .medSøknadsperioder(List.of(new Periode(fom, tom)))
            .medSatsperioder(satsperioder)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medTilkjentYtelse(tilkjentYtelse)
            .medAldersvilkår(new LocalDateTimeline<>(p, Utfall.OPPFYLT))
            .medFødselsdato(fødselsdato)
            .medTriggere(Set.of(new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, DatoIntervallEntitet.fra(p))))
            .medInngangsvilkårVurderinger(inngangsvilkårVurderinger)
            .medVilkår(vilkårType, bostedVilkårTidslinje)
            .medBostedsAvklaringer(List.of(BostedsAvklaringTestData.avslag(avslåttVilkårPeriode, ikkeOppfyltÅrsak)))
            .build();
    }
}
