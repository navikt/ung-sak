package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

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
import no.nav.k9.sak.domene.arbeidsforhold.impl.SakInntektsmeldinger;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.RefusjonskravDato;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef("*")
public class KalkulatorInputTjeneste {

    private Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste;

    @Inject
    public KalkulatorInputTjeneste(@Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste) {
        this.opptjeningForBeregningTjeneste = Objects.requireNonNull(opptjeningForBeregningTjeneste, "opptjeningForBeregningTjeneste");
    }

    protected KalkulatorInputTjeneste() {
        // for CDI proxy
    }

    public KalkulatorInputDto byggDto(BehandlingReferanse referanse,
                                      InntektArbeidYtelseGrunnlag iayGrunnlag,
                                      SakInntektsmeldinger sakInntektsmeldinger,
                                      List<RefusjonskravDato> refusjonskravDatoer,
                                      YtelsespesifiktGrunnlagDto ytelseGrunnlag,
                                      DatoIntervallEntitet vilkårsperiode) {
        var stp = finnSkjæringstidspunkt(vilkårsperiode);

        OpptjeningForBeregningTjeneste tjeneste = finnOpptjeningForBeregningTjeneste(referanse);

        var grunnlagDto = mapIAYTilKalkulus(referanse, vilkårsperiode, iayGrunnlag, sakInntektsmeldinger, tjeneste.finnOppgittOpptjening(iayGrunnlag).orElse(null));
        var opptjeningAktiviteter = tjeneste.hentEksaktOpptjeningForBeregning(referanse, iayGrunnlag, vilkårsperiode);
        var opptjeningAktiviteterDto = TilKalkulusMapper.mapTilDto(opptjeningAktiviteter);

        KalkulatorInputDto kalkulatorInputDto = new KalkulatorInputDto(grunnlagDto, opptjeningAktiviteterDto, stp);

        if (!refusjonskravDatoer.isEmpty()) {
            kalkulatorInputDto.medRefusjonskravDatoer(TilKalkulusMapper.mapTilDto(refusjonskravDatoer));
        }

        kalkulatorInputDto.medYtelsespesifiktGrunnlag(ytelseGrunnlag);
        return kalkulatorInputDto;
    }

    protected InntektArbeidYtelseGrunnlagDto mapIAYTilKalkulus(BehandlingReferanse referanse,
                                                               DatoIntervallEntitet vilkårsperiode,
                                                               InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag,
                                                               SakInntektsmeldinger sakInntektsmeldinger,
                                                               OppgittOpptjening oppgittOpptjening) {
        return new TilKalkulusMapper().mapTilDto(inntektArbeidYtelseGrunnlag, sakInntektsmeldinger, referanse.getAktørId(), vilkårsperiode, oppgittOpptjening);
    }

    protected LocalDate finnSkjæringstidspunkt(DatoIntervallEntitet vilkårsperiode) {
        return vilkårsperiode.getFomDato();
    }

    private OpptjeningForBeregningTjeneste finnOpptjeningForBeregningTjeneste(BehandlingReferanse referanse) {
        FagsakYtelseType ytelseType = referanse.getFagsakYtelseType();
        var tjeneste = FagsakYtelseTypeRef.Lookup.find(opptjeningForBeregningTjeneste, ytelseType)
                .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpptjeningForBeregningTjeneste.class.getSimpleName() + " for ytelseType=" + ytelseType));
        return tjeneste;
    }
}
