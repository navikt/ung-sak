package no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
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

    private static AktivitetspengerTestScenario opphørMedÅrsak(LocalDate fom, VilkårType vilkårType, BostedsvilkårIkkeOppfyltÅrsak ikkeOppfyltÅrsak, String fritekstTilBrev, BostedsavklaringKildeType kilde, String kildeFritekst) {
        LocalDate fødselsdato = fom.minusYears(20);
        var tom = fom.plusWeeks(52).minusDays(1);
        var p = new LocalDateInterval(fom, tom);

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

        LocalDate opphørDato = fom.plusMonths(3);
        var opphørtVilkårPeriode = new Periode(opphørDato, tom);

        var inngangsvilkårVurderinger = InngangsvilkårVurderingTestData.builder()
            .medBostedsvilkårResultat(opphørtVilkårPeriode, false, ikkeOppfyltÅrsak, fritekstTilBrev)
            .build();

        var bostedVilkårTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(opphørtVilkårPeriode.getFom(), opphørtVilkårPeriode.getTom(),
                VilkårUtfall.avslått(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, fritekstTilBrev))
        ));

        return AktivitetspengerTestScenario.builder()
            .medNavn(DEFAULT_NAVN)
            .medSøknadsperioder(List.of(new Periode(fom, tom)))
            .medSatsperioder(satsperioder)
            .medBeregningsgrunnlag(beregningsgrunnlag)
            .medTilkjentYtelse(tilkjentYtelsePerioder(lagSatserTidslinje(satsGrunnlagTidslinje, beregningsgrunnlag), new LocalDateInterval(fom, opphørDato.minusDays(1))))
            .medAldersvilkår(new LocalDateTimeline<>(p, Utfall.OPPFYLT))
            .medFødselsdato(fødselsdato)
            .medTriggere(Set.of(new Trigger(BehandlingÅrsakType.ENDRET_BOSTED, DatoIntervallEntitet.fra(p))))
            .medInngangsvilkårVurderinger(inngangsvilkårVurderinger)
            .medVilkår(vilkårType, bostedVilkårTidslinje)
            .medBostedsAvklaringer(List.of(BostedsAvklaringTestData.opphør(opphørtVilkårPeriode, ikkeOppfyltÅrsak).medKilde(kilde, kildeFritekst)))
            .build();
    }
}

