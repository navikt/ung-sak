package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef("*")
public class KalkulatorInputTjeneste {

    private Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste;
    private Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning;

    @Inject
    public KalkulatorInputTjeneste(@Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                   @Any Instance<InntektsmeldingerRelevantForBeregning> inntektsmeldingerRelevantForBeregning) {
        this.opptjeningForBeregningTjeneste = Objects.requireNonNull(opptjeningForBeregningTjeneste, "opptjeningForBeregningTjeneste");
        this.inntektsmeldingerRelevantForBeregning = inntektsmeldingerRelevantForBeregning;
    }

    protected KalkulatorInputTjeneste() {
        // for CDI proxy
    }

    public KalkulatorInputDto byggDto(BehandlingReferanse referanse,
                                      UUID bgReferanse,
                                      InntektArbeidYtelseGrunnlag iayGrunnlag,
                                      Collection<Inntektsmelding> sakInntektsmeldinger,
                                      YtelsespesifiktGrunnlagDto ytelseGrunnlag,
                                      DatoIntervallEntitet vilkårsperiode) {
        var stp = finnSkjæringstidspunkt(vilkårsperiode);

        OpptjeningForBeregningTjeneste tjeneste = finnOpptjeningForBeregningTjeneste(referanse);
        var imTjeneste = finnInntektsmeldingForBeregningTjeneste(referanse);

        var grunnlagDto = mapIAYTilKalkulus(referanse, vilkårsperiode, iayGrunnlag, sakInntektsmeldinger, tjeneste.finnOppgittOpptjening(iayGrunnlag).orElse(null), imTjeneste);
        var opptjeningAktiviteter = tjeneste.hentEksaktOpptjeningForBeregning(referanse, iayGrunnlag, vilkårsperiode);

        if (opptjeningAktiviteter.isEmpty()) {
            OppgittOpptjening oppgittOpptjening = iayGrunnlag.getOppgittOpptjening().orElse(null);
            throw new IllegalStateException("Forventer opptjening for vilkårsperiode: " + vilkårsperiode + ", bgReferanse=" + bgReferanse + ", iayGrunnlag.opptjening=" + oppgittOpptjening);
        }

        var opptjeningAktiviteterDto = TilKalkulusMapper.mapTilDto(opptjeningAktiviteter.get());

        KalkulatorInputDto kalkulatorInputDto = new KalkulatorInputDto(grunnlagDto, opptjeningAktiviteterDto, stp);

        kalkulatorInputDto.medYtelsespesifiktGrunnlag(ytelseGrunnlag);
        return kalkulatorInputDto;
    }

    protected InntektArbeidYtelseGrunnlagDto mapIAYTilKalkulus(BehandlingReferanse referanse,
                                                               DatoIntervallEntitet vilkårsperiode,
                                                               InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag,
                                                               Collection<Inntektsmelding> inntektsmeldinger,
                                                               OppgittOpptjening oppgittOpptjening,
                                                               InntektsmeldingerRelevantForBeregning imTjeneste) {
        return new TilKalkulusMapper().mapTilDto(inntektArbeidYtelseGrunnlag, inntektsmeldinger, referanse.getAktørId(), vilkårsperiode, oppgittOpptjening, imTjeneste);
    }

    protected LocalDate finnSkjæringstidspunkt(DatoIntervallEntitet vilkårsperiode) {
        return vilkårsperiode.getFomDato();
    }

    private OpptjeningForBeregningTjeneste finnOpptjeningForBeregningTjeneste(BehandlingReferanse referanse) {
        FagsakYtelseType ytelseType = referanse.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(opptjeningForBeregningTjeneste, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningForBeregningTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

    private InntektsmeldingerRelevantForBeregning finnInntektsmeldingForBeregningTjeneste(BehandlingReferanse referanse) {
        FagsakYtelseType ytelseType = referanse.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(inntektsmeldingerRelevantForBeregning, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + InntektsmeldingerRelevantForBeregning.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }
}
