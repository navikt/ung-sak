package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.omsorgenfor.regelmodell;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import no.nav.fpsak.nare.doc.RuleDocumentation;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.specification.LeafSpecification;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.BostedsAdresse;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.Fosterbarn;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForAvslagsårsaker;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForVilkårGrunnlag;

@RuleDocumentation(HarSøkerOmsorgenForBarn.ID)
public class HarSøkerOmsorgenForBarn extends LeafSpecification<OmsorgenForVilkårGrunnlag> {

    static final String ID = "OMS_VK_9.5.1_2";

    HarSøkerOmsorgenForBarn() {
        super(ID);
    }

    @Override
    public Evaluation evaluate(OmsorgenForVilkårGrunnlag grunnlag) {

        if (grunnlag.getHarBlittVurdertSomOmsorgsPerson() != null && grunnlag.getHarBlittVurdertSomOmsorgsPerson()) {
            grunnlag.leggTilUtfall(grunnlag.getVilkårsperiode(), Utfall.OPPFYLT);
            return ja();
        }

        var tidslinje = new LocalDateTimeline<>(grunnlag.getVilkårsperiode().toLocalDateInterval(), Utfall.IKKE_OPPFYLT);

        tidslinje = tidslinje.combine(perioderMedLikeAdresser(grunnlag.getSøkersAdresser(), grunnlag.getPleietrengendeAdresser()), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        tidslinje = tidslinje.combine(perioderMedLikeAdresser(grunnlag.getSøkersAdresser(), grunnlag.getDeltBostedsAdresser()), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
        tidslinje = tidslinje.combine(perioderMedFosterbarn(grunnlag), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);

        tidslinje.forEach(segment -> grunnlag.leggTilUtfall(segment.getLocalDateInterval(), segment.getValue()));

        if (tidslinje.stream().allMatch(it -> Objects.equals(it.getValue(), Utfall.IKKE_OPPFYLT))) {
            return nei(OmsorgenForAvslagsårsaker.IKKE_DOKUMENTERT_OMSORGEN_FOR.toRuleReason());
        }

        return ja();
    }

    private LocalDateTimeline<Utfall> perioderMedFosterbarn(OmsorgenForVilkårGrunnlag grunnlag) {
        var segmenter = new ArrayList<LocalDateSegment<Utfall>>();
        for (Fosterbarn fosterbarn : grunnlag.getFosterbarn()) {
            var fosterbarnPeriode = DatoIntervallEntitet.fra(fosterbarn.getFødselsdato(), fosterbarn.getDødsdato());
            if (fosterbarnPeriode.overlapper(grunnlag.getVilkårsperiode())) {
                segmenter.add(new LocalDateSegment<>(fosterbarnPeriode.overlapp(grunnlag.getVilkårsperiode()).toLocalDateInterval(), Utfall.OPPFYLT));
            }
        }
        return new LocalDateTimeline<>(segmenter);
    }

    private LocalDateTimeline<Utfall> perioderMedLikeAdresser(List<BostedsAdresse> søkersAdresser, List<BostedsAdresse> barnsAdresser) {
        var utfallsTidslinje = new LocalDateTimeline<Utfall>(List.of());

        for (BostedsAdresse søkersAdresse : søkersAdresser) {
            for (BostedsAdresse barnsAdresse: barnsAdresser) {
                if (søkersAdresse.getPeriode().overlapper(barnsAdresse.getPeriode()) && søkersAdresse.erSammeAdresse(barnsAdresse)) {
                    var segment = new LocalDateSegment<>(søkersAdresse.getPeriode().overlapp(barnsAdresse.getPeriode()).toLocalDateInterval(), Utfall.OPPFYLT);
                    utfallsTidslinje = utfallsTidslinje.combine(segment, StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
                }
            }
        }
        return utfallsTidslinje;
    }
}
