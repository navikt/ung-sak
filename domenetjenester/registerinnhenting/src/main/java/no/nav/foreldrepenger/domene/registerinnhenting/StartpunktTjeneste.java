package no.nav.foreldrepenger.domene.registerinnhenting;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;

public interface StartpunktTjeneste {

    // FIXME K9 må håndtere diff for endringer (hvis påvirker sak) i mottak av søknad (kalles ikke p.t.)
    StartpunktType utledStartpunktMotOriginalBehandling(BehandlingReferanse revurdering);
    StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, EndringsresultatDiff differanse);
}
