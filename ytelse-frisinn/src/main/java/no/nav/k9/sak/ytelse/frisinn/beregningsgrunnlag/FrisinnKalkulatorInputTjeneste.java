package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.GrunnbeløpTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulatorInputTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.OpptjeningForBeregningTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.v1.TilKalkulusMapper;
import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.folketrygdloven.kalkulus.felles.v1.KalkulatorInputDto;
import no.nav.folketrygdloven.kalkulus.iay.v1.InntektArbeidYtelseGrunnlagDto;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.iay.modell.OppgittOpptjening;
import no.nav.k9.sak.domene.iay.modell.RefusjonskravDato;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@ApplicationScoped
@FagsakYtelseTypeRef("FRISINN")
public class FrisinnKalkulatorInputTjeneste extends KalkulatorInputTjeneste {

    @Inject
    public FrisinnKalkulatorInputTjeneste(InntektArbeidYtelseTjeneste iayTjeneste,
                                          @Any Instance<OpptjeningForBeregningTjeneste> opptjeningForBeregningTjeneste,
                                          GrunnbeløpTjeneste grunnbeløpTjeneste) {
        super(iayTjeneste, opptjeningForBeregningTjeneste, grunnbeløpTjeneste);
    }

    protected FrisinnKalkulatorInputTjeneste() {
        // for CDI proxy
    }

    @Override
    protected TilKalkulusMapper getTilKalkulusMapper() {
        return new FrisinnTilKalkulusMapper();
    }

    @Override
    protected LocalDate finnSkjæringstidspunkt(DatoIntervallEntitet vilkårsperiode) {
        return LocalDate.of(2020, 3, 1);
    }

}
