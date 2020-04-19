package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.UtledBeregningSkjæringstidspunktForBehandlingTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef()
public class DefaultUtledBeregningSkjæringstidspunktForBehandlingTjeneste implements UtledBeregningSkjæringstidspunktForBehandlingTjeneste {

    @Override
    public LocalDate utled(BehandlingReferanse referanse) {
        return referanse.getUtledetSkjæringstidspunkt();
    }
}
