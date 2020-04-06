package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulus.beregning.v1.YtelsespesifiktGrunnlagDto;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagYtelsespesifiktGrunnlagMapper;

@SuppressWarnings("rawtypes")
@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class FrisinnYtelsesspesifiktGrunnlagMapper implements BeregningsgrunnlagYtelsespesifiktGrunnlagMapper {

    @Inject
    public FrisinnYtelsesspesifiktGrunnlagMapper() {
    }

    @Override
    public YtelsespesifiktGrunnlagDto lagYtelsespesifiktGrunnlag(BehandlingReferanse ref) {
        throw new UnsupportedOperationException("Ikke implementert for FRISINN ennå");
    }


}
