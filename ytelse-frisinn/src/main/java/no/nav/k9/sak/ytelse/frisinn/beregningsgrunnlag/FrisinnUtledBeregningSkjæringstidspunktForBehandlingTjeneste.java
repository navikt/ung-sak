package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import java.time.LocalDate;

import javax.enterprise.context.Dependent;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.UtledBeregningSkjæringstidspunktForBehandlingTjeneste;

@Dependent
@FagsakYtelseTypeRef("FRISINN")
public class FrisinnUtledBeregningSkjæringstidspunktForBehandlingTjeneste implements UtledBeregningSkjæringstidspunktForBehandlingTjeneste {

    @Override
    public LocalDate utled(BehandlingReferanse referanse) {
        return referanse.getUtledetSkjæringstidspunkt();
    }
}
