package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.FinnYrkesaktiviteterForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapArbeidsforholdFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningRefusjonOverstyringerEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.refusjon.InntektsmeldingMedRefusjonTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.ArbeidsforholdOgInntektsmelding;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Periode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.PeriodeModell;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.SplittetPeriode;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.aktivitet.AktivitetStatus;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;

public abstract class MapFastsettBeregningsgrunnlagPerioderFraVLTilRegel {

    private InntektsmeldingMedRefusjonTjeneste inntektsmeldingMedRefusjonTjeneste;

    protected MapFastsettBeregningsgrunnlagPerioderFraVLTilRegel() {
    }

    protected MapFastsettBeregningsgrunnlagPerioderFraVLTilRegel(InntektsmeldingMedRefusjonTjeneste inntektsmeldingMedRefusjonTjeneste) {
        this.inntektsmeldingMedRefusjonTjeneste = inntektsmeldingMedRefusjonTjeneste;
    }

    protected abstract ArbeidsforholdOgInntektsmelding.Builder mapInntektsmelding(Collection<Inntektsmelding>inntektsmeldinger,
                                                                                  Collection<AndelGradering> andelGraderinger,
                                                                                  Map<Arbeidsgiver, LocalDate> førsteIMMap,
                                                                                  Yrkesaktivitet ya,
                                                                                  LocalDate startdatoPermisjon,
                                                                                  ArbeidsforholdOgInntektsmelding.Builder builder,
                                                                                  Optional<BeregningRefusjonOverstyringerEntitet> beregningRefusjonOverstyringer);

    protected void precondition(@SuppressWarnings("unused") BeregningsgrunnlagEntitet beregningsgrunnlag) {
        // template method
    }

    public PeriodeModell map(BeregningsgrunnlagInput input,
                             BeregningsgrunnlagEntitet beregningsgrunnlag) {
        precondition(beregningsgrunnlag);
        var ref = input.getBehandlingReferanse();
        AktørId aktørId = ref.getAktørId();
        var iayGrunnlag = input.getIayGrunnlag();
        var filter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId));
        var førsteIMMap = inntektsmeldingMedRefusjonTjeneste.finnFørsteInntektsmeldingMedRefusjon(ref);
        var graderinger = input.getAktivitetGradering().getAndelGradering();

        LocalDate skjæringstidspunkt = beregningsgrunnlag.getSkjæringstidspunkt();

        var beregningsgrunnlagPeriode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        var andeler = beregningsgrunnlagPeriode.getBeregningsgrunnlagPrStatusOgAndelList();
        var eksisterendePerioder = beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .map(MapSplittetPeriodeFraVLTilRegel::map).collect(Collectors.toList());

        var grunnlag = input.getBeregningsgrunnlagGrunnlag();
        var regelInntektsmeldinger = mapInntektsmeldinger(ref,
            iayGrunnlag,
            graderinger,
            filter,
            andeler,
            skjæringstidspunkt,
            førsteIMMap,
            grunnlag);
        var regelAndelGraderinger = graderinger.stream()
            .filter(ag -> !AktivitetStatus.ARBEIDSTAKER.equals(ag.getAktivitetStatus()))
            .map(MapAndelGradering::mapTilRegelAndelGradering)
            .collect(Collectors.toList());

        return mapPeriodeModell(ref,
            beregningsgrunnlag,
            skjæringstidspunkt,
            eksisterendePerioder,
            regelInntektsmeldinger,
            List.copyOf(regelAndelGraderinger));
    }

    protected abstract PeriodeModell mapPeriodeModell(BehandlingReferanse ref,
                                                      BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                      LocalDate skjæringstidspunkt,
                                                      List<SplittetPeriode> eksisterendePerioder,
                                                      List<ArbeidsforholdOgInntektsmelding> regelInntektsmeldinger,
                                                      List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AndelGradering> regelAndelGraderinger);

    private List<ArbeidsforholdOgInntektsmelding> mapInntektsmeldinger(BehandlingReferanse referanse,
                                                                       InntektArbeidYtelseGrunnlag iayGrunnlag,
                                                                       Collection<AndelGradering> andelGraderinger,
                                                                       YrkesaktivitetFilter filter,
                                                                       List<BeregningsgrunnlagPrStatusOgAndel> andeler,
                                                                       LocalDate skjæringstidspunktBeregning,
                                                                       Map<Arbeidsgiver, LocalDate> førsteIMMap,
                                                                       BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        Collection<Inntektsmelding>inntektsmeldinger = iayGrunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .orElse(Collections.emptyList());
        List<ArbeidsforholdOgInntektsmelding> list = new ArrayList<>();
        FinnYrkesaktiviteterForBeregningTjeneste.finnYrkesaktiviteter(referanse, iayGrunnlag, grunnlag)
            .forEach(ya -> {
                Periode ansettelsesPeriode = FinnAnsettelsesPeriode.finnMinMaksPeriode(filter.getAnsettelsesPerioder(ya), skjæringstidspunktBeregning).get();
                Optional<LocalDate> førstedagEtterBekreftetPermisjonOpt = FinnFørsteDagEtterBekreftetPermisjon.finn(
                    iayGrunnlag, ya, ansettelsesPeriode, skjæringstidspunktBeregning);
                if (!førstedagEtterBekreftetPermisjonOpt.isPresent()) {
                    return;
                }
                Optional<BeregningsgrunnlagPrStatusOgAndel> matchendeAndel = andeler.stream()
                    .filter(andel -> andel.gjelderSammeArbeidsforhold(ya.getArbeidsgiver(), ya.getArbeidsforholdRef()))
                    .findFirst();
                LocalDate førstedagEtterBekreftetPermisjon = førstedagEtterBekreftetPermisjonOpt.get();
                boolean harOpprettetAndelForArbeidsforhold = matchendeAndel.isPresent();
                LocalDate startdatoPermisjon = FinnStartdatoPermisjon.finnStartdatoPermisjon(ya, skjæringstidspunktBeregning,
                    førstedagEtterBekreftetPermisjon, inntektsmeldinger);
                Arbeidsforhold arbeidsforhold = MapArbeidsforholdFraVLTilRegel.mapArbeidsforhold(ya.getArbeidsgiver(), ya.getArbeidsforholdRef());
                ArbeidsforholdOgInntektsmelding.Builder builder = ArbeidsforholdOgInntektsmelding.builder();
                if (harOpprettetAndelForArbeidsforhold) {
                    BeregningsgrunnlagPrStatusOgAndel bgAndel = matchendeAndel.get();
                    boolean gjelderSpesifikk = bgAndel.getBgAndelArbeidsforhold()
                        .filter(bga -> bga.getArbeidsforholdRef().gjelderForSpesifiktArbeidsforhold()).isPresent();
                    if (!gjelderSpesifikk && startdatoPermisjon.isAfter(skjæringstidspunktBeregning)) {
                        return;
                    }
                    builder.medAndelsnr(bgAndel.getAndelsnr());
                }
                mapInntektsmelding(inntektsmeldinger, andelGraderinger, førsteIMMap, ya, startdatoPermisjon, builder, grunnlag.getRefusjonOverstyringer());

                ArbeidsforholdOgInntektsmelding arbeidsforholdOgInntektsmelding = builder
                    .medAnsettelsesperiode(ansettelsesPeriode)
                    .medArbeidsforhold(arbeidsforhold)
                    .medStartdatoPermisjon(startdatoPermisjon)
                    .build();
                list.add(arbeidsforholdOgInntektsmelding);
            });
        return list;
    }

}
