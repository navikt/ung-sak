package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserAvklaringKildeType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
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

public class AktivitetspengerOpphørScenarioer {

    public static AktivitetspengerTestScenario opphørPgaBosted(LocalDate fom) {
        return opphørMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM, null, BostedsavklaringKildeType.BRUKER, null);
    }

    public static AktivitetspengerTestScenario opphørPgaBostedAnnet(LocalDate fom, String fritekstTilBrev) {
        return opphørMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.ANNET, fritekstTilBrev, BostedsavklaringKildeType.ANNET, "veileder ved Nav Trondheim");
    }

    public static AktivitetspengerTestScenario opphørPgaBostedFolkeregistrert(LocalDate fom) {
        return opphørMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSTEDSADRESSE_OG_IKKE_FOLKEREGISTRERT_I_TRONDHEIM, null, BostedsavklaringKildeType.FOLKEREGISTER, null);
    }

    public static AktivitetspengerTestScenario opphørPgaArbeidsstedStudiested(LocalDate fom) {
        return opphørMedÅrsak(fom, VilkårType.BOSTEDSVILKÅR, BostedsvilkårIkkeOppfyltÅrsak.STUDIE_ELLER_ARBEIDSSTED_UTENFOR_TRONDHEIM, null, BostedsavklaringKildeType.BRUKER, null);
    }

    public static AktivitetspengerTestScenario opphørPgaAndreLivsoppholdsytelser(LocalDate fom,
                                                                                 AndreLivsoppholdsytelserIkkeOppfyltÅrsak ikkeOppfyltÅrsak,
                                                                                 AndreLivsoppholdsytelserAvklaringKildeType kilde,
                                                                                 String fritekstTilBrev) {
        var opphørtVilkårPeriode = opphørtPeriode(fom);

        var vurderinger = InngangsvilkårVurderingTestData.builder()
            .medAndreYtelser(opphørtVilkårPeriode, false, ikkeOppfyltÅrsak, fritekstTilBrev)
            .build();

        return opphørBuilder(fom, opphørtVilkårPeriode)
            .medTriggere(Set.of(new Trigger(BehandlingÅrsakType.ENDRET_LIVSOPPHOLDSYTELSE, DatoIntervallEntitet.fra(lagPeriodeMedEttÅrFra(fom)))))
            .medInngangsvilkårVurderinger(vurderinger)
            .medVilkår(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR,
                avslåttTidslinje(opphørtVilkårPeriode, Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, fritekstTilBrev))
            .medVilkårsavklaringer(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR,
                List.of(VilkårsavklaringTestData.opphør(opphørtVilkårPeriode, ikkeOppfyltÅrsak, kilde)))
            .build();
    }

    public static AktivitetspengerTestScenario opphørPgaBostedOgAndreLivsoppholdsytelser(LocalDate fom) {
        var opphørtVilkårPeriode = opphørtPeriode(fom);
        var bostedsÅrsak = BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM;
        var livsoppholdsÅrsak = AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER;

        var vurderinger = InngangsvilkårVurderingTestData.builder()
            .medBostedsvilkårResultat(opphørtVilkårPeriode, false, bostedsÅrsak, null)
            .medAndreYtelser(opphørtVilkårPeriode, false, livsoppholdsÅrsak, null)
            .build();

        return opphørBuilder(fom, opphørtVilkårPeriode)
            .medTriggere(Set.of(
                new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, DatoIntervallEntitet.fra(lagPeriodeMedEttÅrFra(fom))),
                new Trigger(BehandlingÅrsakType.ENDRET_LIVSOPPHOLDSYTELSE, DatoIntervallEntitet.fra(lagPeriodeMedEttÅrFra(fom)))))
            .medInngangsvilkårVurderinger(vurderinger)
            .medVilkår(VilkårType.BOSTEDSVILKÅR,
                avslåttTidslinje(opphørtVilkårPeriode, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, null))
            .medVilkår(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR,
                avslåttTidslinje(opphørtVilkårPeriode, Avslagsårsak.SØKER_HAR_ANNEN_LIVSOPPHOLDSYTELSE, null))
            .medBostedsAvklaringer(List.of(BostedsAvklaringTestData.opphør(opphørtVilkårPeriode, bostedsÅrsak)))
            .medVilkårsavklaringer(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR,
                List.of(VilkårsavklaringTestData.opphør(opphørtVilkårPeriode, livsoppholdsÅrsak, AndreLivsoppholdsytelserAvklaringKildeType.BRUKER)))
            .build();
    }

    private static AktivitetspengerTestScenario opphørMedÅrsak(LocalDate fom, VilkårType vilkårType, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev, BostedsavklaringKildeType kilde, String kildeFritekst) {
        var opphørtVilkårPeriode = opphørtPeriode(fom);

        var inngangsvilkårVurderinger = InngangsvilkårVurderingTestData.builder()
            .medBostedsvilkårResultat(opphørtVilkårPeriode, false, ikkeOppfyltÅrsak, fritekstTilBrev)
            .build();

        return opphørBuilder(fom, opphørtVilkårPeriode)
            .medTriggere(Set.of(new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, DatoIntervallEntitet.fra(lagPeriodeMedEttÅrFra(fom)))))
            .medInngangsvilkårVurderinger(inngangsvilkårVurderinger)
            .medVilkår(vilkårType, avslåttTidslinje(opphørtVilkårPeriode, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, fritekstTilBrev))
            .medBostedsAvklaringer(List.of(BostedsAvklaringTestData.opphør(opphørtVilkårPeriode, ikkeOppfyltÅrsak).medKilde(kilde, kildeFritekst)))
            .build();
    }

    private static LocalDateInterval lagPeriodeMedEttÅrFra(LocalDate fom) {
        return new LocalDateInterval(fom, fom.plusWeeks(52).minusDays(1));
    }

    private static Periode opphørtPeriode(LocalDate fom) {
        return new Periode(fom.plusMonths(3), lagPeriodeMedEttÅrFra(fom).getTomDato());
    }

    private static LocalDateTimeline<VilkårUtfall> avslåttTidslinje(Periode periode, Avslagsårsak avslagsårsak, String fritekstTilBrev) {
        return new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(periode.getFom(), periode.getTom(), VilkårUtfall.avslått(avslagsårsak, fritekstTilBrev))
        ));
    }

    private static AktivitetspengerTestScenario.Builder opphørBuilder(LocalDate fom, Periode opphørtVilkårPeriode) {
        LocalDate fødselsdato = fom.minusYears(20);
        var p = lagPeriodeMedEttÅrFra(fom);
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

        return AktivitetspengerTestScenario.builder()
            .medNavn(DEFAULT_NAVN)
            .medSøknadsperioder(List.of(new Periode(fom, tom)))
            .medSatsperioder(satsperioder)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medTilkjentYtelse(tilkjentYtelsePerioder(lagSatserTidslinje(satsGrunnlagTidslinje, beregningsgrunnlag),
                new LocalDateInterval(fom, opphørtVilkårPeriode.getFom().minusDays(1))))
            .medAldersvilkår(new LocalDateTimeline<>(p, Utfall.OPPFYLT))
            .medFødselsdato(fødselsdato);
    }
}
