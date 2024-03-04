package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningsaktiviteterPerYtelse;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.opptjening.OpptjeningAktivitetType;
import no.nav.k9.kodeverk.vilkår.VilkårUtfallMerknad;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.Opptjening;
import no.nav.k9.sak.domene.iay.modell.Opptjeningsnøkkel;
import no.nav.k9.sak.domene.opptjening.OpptjeningsperiodeForSaksbehandling;
import no.nav.k9.sak.domene.opptjening.VurderingsStatus;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class FiltrerOpptjeningaktivitetForBeregning {

    private static final OpptjeningsaktiviteterPerYtelse opptjeningsaktiviteter = new OpptjeningsaktiviteterPerYtelse(Set.of(
        OpptjeningAktivitetType.VIDERE_ETTERUTDANNING,
        OpptjeningAktivitetType.UTENLANDSK_ARBEIDSFORHOLD,
        OpptjeningAktivitetType.ARBEIDSAVKLARING));


    public static List<OpptjeningsperiodeForSaksbehandling> filtrerForBeregning(DatoIntervallEntitet vilkårsperiode,
                                                                         List<OpptjeningsperiodeForSaksbehandling> opptjeningAktivitetPerioder,
                                                                         VilkårUtfallMerknad vilkårUtfallMerknad,
                                                                         Opptjening opptjening) {
        var perioder = slåSammenLikeVurderinger(opptjeningAktivitetPerioder);
        return perioder.stream()
            .filter(oa -> filtrerForVilkårsperiode(vilkårsperiode, oa, vilkårUtfallMerknad))
            .filter(oa -> !oa.getPeriode().getTomDato().isBefore(opptjening.getFom()))
            .filter(oa -> opptjeningsaktiviteter.erRelevantAktivitet(oa.getOpptjeningAktivitetType()))
            .filter(oa -> oa.getVurderingsStatus().equals(VurderingsStatus.GODKJENT))
            .collect(Collectors.toList());
    }

    private static List<OpptjeningsperiodeForSaksbehandling> slåSammenLikeVurderinger(List<OpptjeningsperiodeForSaksbehandling> aktiviteter) {
        var gruppertPåNøkkel = aktiviteter.stream().collect(Collectors.groupingBy(
            a -> new Grupperingsnøkkel(a.getOpptjeningsnøkkel(), a.getOpptjeningAktivitetType())
        ));

        return gruppertPåNøkkel.entrySet()
            .stream()
            .filter(e -> !e.getValue().isEmpty())
            .flatMap(e -> {
                var segmenter = e.getValue().stream().map(v -> new LocalDateSegment<>(v.getPeriode().getFomDato(), v.getPeriode().getTomDato(), v.getVurderingsStatus())).toList();
                var tidslinje = new LocalDateTimeline<>(segmenter, StandardCombinators::coalesceLeftHandSide);
                var first = e.getValue().get(0);
                return tidslinje.compress().toSegments().stream().map(s ->
                    OpptjeningsperiodeForSaksbehandling.Builder.kopi(first)
                        .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(s.getFom(), s.getTom()))
                        .medVurderingsStatus(s.getValue()).build()
                );
            }).toList();
    }

    private static boolean filtrerForVilkårsperiode(DatoIntervallEntitet vilkårsperiode, OpptjeningsperiodeForSaksbehandling oa, VilkårUtfallMerknad vilkårUtfallMerknad) {
        return oa.getPeriode().getFomDato().isBefore(vilkårsperiode.getFomDato()) || (erInaktiv(vilkårUtfallMerknad) && oa.getPeriode().getFomDato().equals(vilkårsperiode.getFomDato()));
    }

    private static boolean erInaktiv(VilkårUtfallMerknad vilkårUtfallMerknad) {
        return VilkårUtfallMerknad.VM_7847_B.equals(vilkårUtfallMerknad) || VilkårUtfallMerknad.VM_7847_A.equals(vilkårUtfallMerknad);
    }

    private record Grupperingsnøkkel(Opptjeningsnøkkel nøkkel, OpptjeningAktivitetType type) { }


}
