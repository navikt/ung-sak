package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.RefusjonskravDato;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef("*")
public class KalkulatorInputTjeneste {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste;
    private GrunnbeløpTjeneste grunnbeløpTjeneste;

    @Inject
    public KalkulatorInputTjeneste(InntektArbeidYtelseTjeneste iayTjeneste,
                                   @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                   GrunnbeløpTjeneste grunnbeløpTjeneste) {
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.opptjeningForBeregningTjeneste = Objects.requireNonNull(opptjeningForBeregningTjeneste, "opptjeningForBeregningTjeneste");
        this.grunnbeløpTjeneste = grunnbeløpTjeneste;
    }

    protected KalkulatorInputTjeneste() {
        // for CDI proxy
    }

    public KalkulatorInputDto byggDto(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag, DatoIntervallEntitet vilkårsperiode) {
        var stp = finnSkjæringstidspunkt(vilkårsperiode);
        var inntektArbeidYtelseGrunnlag = iayTjeneste.hentGrunnlag(referanse.getBehandlingId());
        var sakInntektsmeldinger = iayTjeneste.hentInntektsmeldinger(referanse.getSaksnummer());
        var grunnbeløpsatser = TilKalkulusMapper.mapGrunnbeløp(grunnbeløpTjeneste.mapGrunnbeløpSatser());

        OpptjeningForBeregningTjeneste tjeneste = finnOpptjeningForBeregningTjeneste(referanse);

        var grunnlagDto = getTilKalkulusMapper().mapTilDto(inntektArbeidYtelseGrunnlag, sakInntektsmeldinger, referanse.getAktørId(), vilkårsperiode, tjeneste.finnOppgittOpptjening(inntektArbeidYtelseGrunnlag).orElse(null));
        var opptjeningAktiviteter = tjeneste.hentEksaktOpptjeningForBeregning(referanse, inntektArbeidYtelseGrunnlag, vilkårsperiode);
        var opptjeningAktiviteterDto = TilKalkulusMapper.mapTilDto(opptjeningAktiviteter);

        KalkulatorInputDto kalkulatorInputDto = new KalkulatorInputDto(grunnbeløpsatser, grunnlagDto, opptjeningAktiviteterDto, stp);

        List<RefusjonskravDato> refusjonskravDatoes = iayTjeneste.hentRefusjonskravDatoerForSak(referanse.getSaksnummer());
        if (!refusjonskravDatoes.isEmpty()) {
            kalkulatorInputDto.medRefusjonskravDatoer(TilKalkulusMapper.mapTilDto(refusjonskravDatoes));
        }

        kalkulatorInputDto.medYtelsespesifiktGrunnlag(ytelseGrunnlag);
        return kalkulatorInputDto;
    }

    protected TilKalkulusMapper getTilKalkulusMapper() {
        return new TilKalkulusMapper();
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
