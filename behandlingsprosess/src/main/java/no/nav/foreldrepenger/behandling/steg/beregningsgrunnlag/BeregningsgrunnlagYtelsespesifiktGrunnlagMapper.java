package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;

@FunctionalInterface
public interface BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<V extends YtelsespesifiktGrunnlagDto> {

    V lagYtelsespesifiktGrunnlag(BehandlingReferanse ref);
}
