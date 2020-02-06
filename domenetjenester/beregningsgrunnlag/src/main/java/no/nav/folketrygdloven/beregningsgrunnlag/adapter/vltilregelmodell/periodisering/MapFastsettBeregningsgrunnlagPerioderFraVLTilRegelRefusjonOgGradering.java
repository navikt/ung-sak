package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import static no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.MapGraderingForYrkesaktivitet.mapGraderingForYrkesaktivitet;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningRefusjonOverstyringEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningRefusjonOverstyringerEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.refusjon.InntektsmeldingMedRefusjonTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.ArbeidsforholdOgInntektsmelding;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Gradering;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodeModell;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.PeriodisertBruttoBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.SplittetPeriode;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.typer.Arbeidsgiver;

/** Default mapper for alle ytelser. */
@FagsakYtelseTypeRef()
@ApplicationScoped
public class MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering extends MapFastsettBeregningsgrunnlagPerioderFraVLTilRegel {


    protected MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering() {
    }

    @Inject
    public MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering(InntektsmeldingMedRefusjonTjeneste finnFørsteInntektsmelding) {
        super(finnFørsteInntektsmelding);
    }

    @Override
    protected ArbeidsforholdOgInntektsmelding.Builder mapInntektsmelding(Collection<Inntektsmelding>inntektsmeldinger,
                                                                         Collection<AndelGradering> andelGraderinger,
                                                                         Map<Arbeidsgiver, LocalDate> førsteIMMap,
                                                                         Yrkesaktivitet ya,
                                                                         LocalDate startdatoPermisjon,
                                                                         ArbeidsforholdOgInntektsmelding.Builder builder, Optional<BeregningRefusjonOverstyringerEntitet> refusjonOverstyringer) {
        Optional<Inntektsmelding> matchendeInntektsmelding = inntektsmeldinger.stream()
            .filter(im -> ya.gjelderFor(im.getArbeidsgiver(), im.getArbeidsforholdRef()))
            .findFirst();
        matchendeInntektsmelding.ifPresent(im ->
            builder.medRefusjonskrav(MapRefusjonskravFraVLTilRegel.periodiserRefusjonsbeløp(im, startdatoPermisjon))
        );
        Optional<LocalDate> førsteMuligeRefusjonsdato = mapFørsteGyldigeDatoForRefusjon(ya, refusjonOverstyringer);
        førsteMuligeRefusjonsdato.ifPresent(builder::medOverstyrtRefusjonsFrist);
        List<Gradering> graderinger = mapGraderingForYrkesaktivitet(andelGraderinger, ya);
        builder.medGraderinger(graderinger);
        builder.medInnsendingsdatoFørsteInntektsmeldingMedRefusjon(førsteIMMap.get(ya.getArbeidsgiver()));
        return builder;
    }

    private Optional<LocalDate> mapFørsteGyldigeDatoForRefusjon(Yrkesaktivitet ya, Optional<BeregningRefusjonOverstyringerEntitet> refusjonOverstyringer) {
        return refusjonOverstyringer.stream().flatMap(s -> s.getRefusjonOverstyringer().stream())
            .filter(o -> o.getArbeidsgiver().equals(ya.getArbeidsgiver()))
            .map(BeregningRefusjonOverstyringEntitet::getFørsteMuligeRefusjonFom)
            .findFirst();
    }

    @Override
    protected PeriodeModell mapPeriodeModell(BehandlingReferanse ref,
                                             BeregningsgrunnlagEntitet vlBeregningsgrunnlag,
                                             LocalDate skjæringstidspunkt,
                                             List<SplittetPeriode> eksisterendePerioder,
                                             List<ArbeidsforholdOgInntektsmelding> regelInntektsmeldinger,
                                             List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.AndelGradering> regelAndelGraderinger) {
        List<PeriodisertBruttoBeregningsgrunnlag> periodiseringBruttoBg = MapPeriodisertBruttoBeregningsgrunnlag.map(vlBeregningsgrunnlag);

        return PeriodeModell.builder()
            .medSkjæringstidspunkt(skjæringstidspunkt)
            .medGrunnbeløp(vlBeregningsgrunnlag.getGrunnbeløp().getVerdi())
            .medInntektsmeldinger(regelInntektsmeldinger)
            .medAndelGraderinger(regelAndelGraderinger)
            .medEndringISøktYtelse(Collections.emptyList())
            .medEksisterendePerioder(eksisterendePerioder)
            .medPeriodisertBruttoBeregningsgrunnlag(periodiseringBruttoBg)
            .build();
    }
}
