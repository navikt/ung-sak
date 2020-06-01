package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

@FunctionalInterface
public interface BeregningsgrunnlagYtelsespesifiktGrunnlagMapper<V extends YtelsespesifiktGrunnlagDto> {

    V lagYtelsespesifiktGrunnlag(BehandlingReferanse ref, DatoIntervallEntitet vilkårsperiode);
}
