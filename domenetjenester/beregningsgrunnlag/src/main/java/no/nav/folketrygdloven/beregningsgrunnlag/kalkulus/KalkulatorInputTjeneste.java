package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.RefusjonskravDato;


@ApplicationScoped
public class KalkulatorInputTjeneste {

    private InntektArbeidYtelseTjeneste iayTjeneste;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private GrunnbeløpTjeneste grunnbeløpTjeneste;

    @Inject
    public KalkulatorInputTjeneste(InntektArbeidYtelseTjeneste iayTjeneste,
                                   OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
                                   GrunnbeløpTjeneste grunnbeløpTjeneste) {
        this.iayTjeneste = Objects.requireNonNull(iayTjeneste, "iayTjeneste");
        this.opptjeningForBeregningTjeneste = Objects.requireNonNull(opptjeningForBeregningTjeneste, "opptjeningForBeregningTjeneste");
        this.grunnbeløpTjeneste = grunnbeløpTjeneste;
    }

    protected KalkulatorInputTjeneste() {
        // for CDI proxy
    }

    public KalkulatorInputDto byggDto(BehandlingReferanse referanse) {
        var inntektArbeidYtelseGrunnlag = iayTjeneste.hentGrunnlag(referanse.getBehandlingId());
        var grunnbeløpsatser = TilKalkulusMapper.mapGrunnbeløp(grunnbeløpTjeneste.mapGrunnbeløpSatser());
        var grunnlagDto = TilKalkulusMapper.mapTilDto(inntektArbeidYtelseGrunnlag, referanse.getAktørId(), referanse.getSkjæringstidspunktOpptjening());

        var opptjeningAktiviteter = opptjeningForBeregningTjeneste.hentEksaktOpptjeningForBeregning(referanse, inntektArbeidYtelseGrunnlag);
        var opptjeningAktiviteterDto = TilKalkulusMapper.mapTilDto(opptjeningAktiviteter);

        KalkulatorInputDto kalkulatorInputDto = new KalkulatorInputDto(grunnbeløpsatser, grunnlagDto, opptjeningAktiviteterDto, referanse.getSkjæringstidspunktOpptjening());

        List<RefusjonskravDato> refusjonskravDatoes = iayTjeneste.hentRefusjonskravDatoerForSak(referanse.getSaksnummer());
        if (!refusjonskravDatoes.isEmpty()) {
            kalkulatorInputDto.medRefusjonskravDatoer(TilKalkulusMapper.mapTilDto(refusjonskravDatoes));
        }

        // TODO(OJR) hva skal denne være for k9?
        kalkulatorInputDto.medYtelsespesifiktGrunnlag(new YtelsespesifiktGrunnlagDto(BigDecimal.valueOf(100), 3));
        return kalkulatorInputDto;
    }
}
