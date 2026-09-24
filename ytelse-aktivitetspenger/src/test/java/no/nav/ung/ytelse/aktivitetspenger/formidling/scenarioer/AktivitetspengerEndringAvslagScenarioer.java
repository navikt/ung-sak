package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserAvklaringKildeType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.BistandsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BistandsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;
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
import no.nav.ung.ytelse.aktivitetspenger.testdata.VilkårsavklaringTestData;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils.*;

/**
 * Scenarioer der et inngangsvilkår er avslått av saksbehandler for en avgrenset periode midt i programperioden
 * (i motsetning til opphør, som varer ut programperioden).
 */
public class AktivitetspengerEndringAvslagScenarioer {

    public static AktivitetspengerTestScenario avslagPgaBosted(LocalDate fom) {
        return avslagMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, null, BostedsavklaringKildeType.BRUKER, null);
    }

    public static AktivitetspengerTestScenario avslagPgaBostedAnnet(LocalDate fom, String fritekstTilBrev) {
        return avslagMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.ANNET, fritekstTilBrev, BostedsavklaringKildeType.ANNET, "veileder ved Nav Trondheim");
    }

    public static AktivitetspengerTestScenario avslagPgaBostedFolkeregistrert(LocalDate fom) {
        return avslagMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM, null, BostedsavklaringKildeType.FOLKEREGISTER, null);
    }

    public static AktivitetspengerTestScenario avslagPgaBostedMedAvkortetHale(LocalDate fom) {
        var maksTom = fom.plusWeeks(52).minusDays(1);
        var avslåttFom = fom.plusMonths(3);
        var avslåttTom = fom.plusMonths(5).minusDays(1);
        var avkortetFom = avslåttTom.plusDays(1);
        var avslåttPeriode = new Periode(avslåttFom, avslåttTom);

        var vurderinger = InngangsvilkårVurderingTestData.builder()
            .medBostedsvilkårResultat(avslåttPeriode, false, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, null)
            .medBostedsvilkårResultat(new Periode(avkortetFom, maksTom), false, BostedsvilkårIkkeOppfyltÅrsak.AVKORTET, null)
            .build();

        var bostedTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(avslåttFom, avslåttTom, VilkårUtfall.avslått(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED)),
            new LocalDateSegment<>(avkortetFom, maksTom, VilkårUtfall.avslått(Avslagsårsak.AVKORTET))
        ));

        return avslagBuilder(fom, avslåttPeriode)
            .medVilkår(VilkårType.BOSTEDSVILKÅR, bostedTidslinje)
            .medInngangsvilkårVurderinger(vurderinger)
            .medTriggere(Set.of(new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, DatoIntervallEntitet.fra(lagPeriodeForEttÅrFra(fom)))))
            .medBostedsAvklaringer(List.of(BostedsAvklaringTestData.avslag(
                avslåttPeriode,
                BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM)))
            .build();
    }

    public static AktivitetspengerTestScenario avslagPgaArbeidsstedStudiested(LocalDate fom) {
        return avslagMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM, null, BostedsavklaringKildeType.BRUKER, null);
    }

    public static AktivitetspengerTestScenario avslagPgaAndreLivsoppholdsytelser(LocalDate fom,
                                                                                 AndreLivsoppholdsytelserIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
                                                                                 AndreLivsoppholdsytelserAvklaringKildeType kilde,
                                                                                 String fritekstTilBrev) {
        var avslåttVilkårPeriode = avslåttPeriode(fom);

        var vurderinger = InngangsvilkårVurderingTestData.builder()
            .medAndreYtelser(avslåttVilkårPeriode, false, ikkeOppfyltÅrsak, fritekstTilBrev)
            .build();

        return avslagBuilder(fom, avslåttVilkårPeriode)
            .medTriggere(Set.of(new Trigger(BehandlingÅrsakType.ENDRET_LIVSOPPHOLDSYTELSE, DatoIntervallEntitet.fra(lagPeriodeForEttÅrFra(fom)))))
            .medInngangsvilkårVurderinger(vurderinger)
            .medVilkår(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR,
                avslåttTidslinje(avslåttVilkårPeriode, Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, fritekstTilBrev))
            .medVilkårsavklaringer(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR,
                List.of(VilkårsavklaringTestData.avslag(avslåttVilkårPeriode, ikkeOppfyltÅrsak, kilde)))
            .build();
    }

    public static AktivitetspengerTestScenario avslagPgaBistand(LocalDate fom,
                                                                BistandsavklaringKildeType kilde,
                                                                String fritekstTilBrev) {
        var avslåttVilkårPeriode = avslåttPeriode(fom);
        var ikkeOppfyltÅrsak = BistandsvilkårIkkeOppfyltÅrsak.IKKE_14A_VEDTAK;

        var vurderinger = InngangsvilkårVurderingTestData.builder()
            .medBistandsvilkårResultat(avslåttVilkårPeriode, false, ikkeOppfyltÅrsak, fritekstTilBrev)
            .build();

        return avslagBuilder(fom, avslåttVilkårPeriode)
            .medTriggere(Set.of(new Trigger(BehandlingÅrsakType.ENDRET_BISTANDSBEHOV, DatoIntervallEntitet.fra(lagPeriodeForEttÅrFra(fom)))))
            .medInngangsvilkårVurderinger(vurderinger)
            .medVilkår(VilkårType.BISTANDSVILKÅR,
                avslåttTidslinje(avslåttVilkårPeriode, Avslagsårsak.IKKE_14A_VEDTAK, fritekstTilBrev))
            .medVilkårsavklaringer(VilkårType.BISTANDSVILKÅR,
                List.of(VilkårsavklaringTestData.avslag(avslåttVilkårPeriode, ikkeOppfyltÅrsak, kilde)))
            .build();
    }

    private static AktivitetspengerTestScenario avslagMedÅrsak(LocalDate fom, VilkårType vilkårType, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev, BostedsavklaringKildeType kilde, String kildeFritekst) {
        var avslåttVilkårPeriode = avslåttPeriode(fom);

        var inngangsvilkårVurderinger = InngangsvilkårVurderingTestData.builder()
            .medBostedsvilkårResultat(avslåttVilkårPeriode, false, ikkeOppfyltÅrsak, fritekstTilBrev)
            .build();

        return avslagBuilder(fom, avslåttVilkårPeriode)
            .medTriggere(Set.of(new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, DatoIntervallEntitet.fra(lagPeriodeForEttÅrFra(fom)))))
            .medInngangsvilkårVurderinger(inngangsvilkårVurderinger)
            .medVilkår(vilkårType, avslåttTidslinje(avslåttVilkårPeriode, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, fritekstTilBrev))
            .medBostedsAvklaringer(List.of(BostedsAvklaringTestData.avslag(avslåttVilkårPeriode, ikkeOppfyltÅrsak).medKilde(kilde, kildeFritekst)))
            .build();
    }

    private static LocalDateInterval lagPeriodeForEttÅrFra(LocalDate fom) {
        return new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
    }

    private static Periode avslåttPeriode(LocalDate fom) {
        return new Periode(fom.plusMonths(3), fom.plusMonths(5).minusDays(1));
    }

    private static LocalDateTimeline<VilkårUtfall> avslåttTidslinje(Periode periode, Avslagsårsak avslagsårsak, String fritekstTilBrev) {
        return new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode.getFom(), periode.getTom(), VilkårUtfall.avslått(avslagsårsak, fritekstTilBrev))
        ));
    }

    private static AktivitetspengerTestScenario.Builder avslagBuilder(LocalDate fom, Periode avslåttVilkårPeriode) {
        LocalDate fødselsdato = fom.minusYears(20);
        var p = lagPeriodeForEttÅrFra(fom);
        var tom = p.getTomDato();

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
        var tilkjentYtelse = tilkjentYtelsePerioder(satserTidslinje, new LocalDateInterval(fom, avslåttVilkårPeriode.getFom().minusDays(1)))
            .crossJoin(tilkjentYtelsePerioder(satserTidslinje, new LocalDateInterval(avslåttVilkårPeriode.getTom().plusDays(1), tom)));

        return AktivitetspengerTestScenario.builder()
            .medNavn(DEFAULT_NAVN)
            .medSøknadsperioder(List.of(new Periode(fom, tom)))
            .medSatsperioder(satsperioder)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medTilkjentYtelse(tilkjentYtelse)
            .medAldersvilkår(new LocalDateTimeline<>(p, Utfall.OPPFYLT))
            .medFødselsdato(fødselsdato);
    }
}
