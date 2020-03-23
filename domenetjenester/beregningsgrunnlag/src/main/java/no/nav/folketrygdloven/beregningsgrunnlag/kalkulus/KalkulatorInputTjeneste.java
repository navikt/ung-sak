package no.nav.folketrygdloven.beregningsgrunnlag.kalkulus;

import java.util.List;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.RefusjonskravDato;


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

    public KalkulatorInputDto byggDto(BehandlingReferanse referanse, YtelsespesifiktGrunnlagDto ytelseGrunnlag) {
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

        kalkulatorInputDto.medYtelsespesifiktGrunnlag(ytelseGrunnlag);
        return kalkulatorInputDto;
    }
}
