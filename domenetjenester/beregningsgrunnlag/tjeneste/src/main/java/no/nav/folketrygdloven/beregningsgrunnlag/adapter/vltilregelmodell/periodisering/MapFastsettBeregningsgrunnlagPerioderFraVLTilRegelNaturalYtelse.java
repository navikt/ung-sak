package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningRefusjonOverstyringerEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.refusjon.InntektsmeldingMedRefusjonTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.ArbeidsforholdOgInntektsmelding;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.PeriodeModell;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.SplittetPeriode;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

@ApplicationScoped
public class MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse extends MapFastsettBeregningsgrunnlagPerioderFraVLTilRegel {

    MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse() {
    }

    @Inject
    public MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse(InntektsmeldingMedRefusjonTjeneste inntektsmeldingMedRefusjonTjeneste) {
        super(inntektsmeldingMedRefusjonTjeneste);
    }

    @Override
    protected ArbeidsforholdOgInntektsmelding.Builder mapInntektsmelding(Collection<Inntektsmelding>inntektsmeldinger, Collection<AndelGradering> andelGraderinger, Map<Arbeidsgiver, LocalDate> førsteIMMap, Yrkesaktivitet ya, LocalDate startdatoPermisjon, ArbeidsforholdOgInntektsmelding.Builder builder, Optional<BeregningRefusjonOverstyringerEntitet> refusjonOverstyringer) {
        Optional<Inntektsmelding> matchendeInntektsmelding = inntektsmeldinger.stream()
            .filter(im -> ya.gjelderFor(im.getArbeidsgiver(), im.getArbeidsforholdRef()))
            .findFirst();
        matchendeInntektsmelding.ifPresent(im -> builder.medNaturalytelser(MapNaturalytelser.mapNaturalytelser(im)));
        return builder;
    }

    @Override
    protected void precondition(BeregningsgrunnlagEntitet vlBeregningsgrunnlag) {
        List<BeregningsgrunnlagPeriode> beregningsgrunnlagPerioder = vlBeregningsgrunnlag.getBeregningsgrunnlagPerioder();
        int antallPerioder = beregningsgrunnlagPerioder.size();
        if (antallPerioder != 1) {
            throw MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse.TjenesteFeil.FEILFACTORY.kanIkkeUtvideMedNyePerioder(antallPerioder).toException();
        }
    }


    @Override
    protected PeriodeModell mapPeriodeModell(BehandlingReferanse ref,
                                             BeregningsgrunnlagEntitet vlBeregningsgrunnlag,
                                             LocalDate skjæringstidspunkt,
                                             List<SplittetPeriode> eksisterendePerioder,
                                             List<ArbeidsforholdOgInntektsmelding> regelInntektsmeldinger,
                                             List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AndelGradering> regelAndelGraderinger) {
        return PeriodeModell.builder()
            .medSkjæringstidspunkt(skjæringstidspunkt)
            .medGrunnbeløp(vlBeregningsgrunnlag.getGrunnbeløp().getVerdi())
            .medInntektsmeldinger(regelInntektsmeldinger)
            .medEksisterendePerioder(eksisterendePerioder)
            .build();
    }

    private interface TjenesteFeil extends DeklarerteFeil {
        MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse.TjenesteFeil FEILFACTORY = FeilFactory.create(MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse.TjenesteFeil.class);

        @TekniskFeil(feilkode = "FP-370605", feilmelding = "Kan bare utvide med nye perioder når det fra før finnes 1 periode, fant %s", logLevel = LogLevel.WARN)
        Feil kanIkkeUtvideMedNyePerioder(int antallPerioder);
    }
}
