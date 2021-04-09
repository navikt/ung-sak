package no.nav.k9.sak.domene.vedtak.ekstern;

import java.util.List;
import java.util.Set;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;

public interface UttakTilOverlappSjekkTjeneste {
    List<FagsakYtelseType> getYtelseTyperSomSjekkesMot();

    Set<LocalDateSegment<Boolean>> hentInnvilgetUttaksplan(BehandlingReferanse ref);
}
