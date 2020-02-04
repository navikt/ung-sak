package no.nav.foreldrepenger.behandling.revurdering.felles;


import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.k9.kodeverk.behandling.KonsekvensForYtelsen;


public interface HarEtablertYtelse {

    boolean vurder(boolean finnesInnvilgetIkkeOpphørtVedtak);

    Behandlingsresultat fastsettForIkkeEtablertYtelse(Behandling revurdering, List<KonsekvensForYtelsen> konsekvenserForYtelsen);

}
