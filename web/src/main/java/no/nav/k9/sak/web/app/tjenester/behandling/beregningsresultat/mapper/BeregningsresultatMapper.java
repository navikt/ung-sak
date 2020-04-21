package no.nav.k9.sak.web.app.tjenester.behandling.beregningsresultat.mapper;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.k9.sak.kontrakt.beregningsresultat.BeregningsresultatDto;

@FunctionalInterface
public interface BeregningsresultatMapper {

    BeregningsresultatDto map(Behandling behandling,
                              BehandlingBeregningsresultatEntitet beregningsresultatAggregat);
}
