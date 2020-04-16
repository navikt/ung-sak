package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import java.time.LocalDate;

import no.nav.k9.sak.behandling.BehandlingReferanse;

@FunctionalInterface
public interface UtledBeregningSkjæringstidspunktForBehandlingTjeneste {

    LocalDate utled(BehandlingReferanse referanse);
}
