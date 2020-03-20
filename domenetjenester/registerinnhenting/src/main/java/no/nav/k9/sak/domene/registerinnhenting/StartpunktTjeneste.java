package no.nav.k9.sak.domene.registerinnhenting;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;

public interface StartpunktTjeneste {

    // FIXME K9 må håndtere diff for endringer (hvis påvirker sak) i mottak av søknad (kalles ikke p.t.)
    StartpunktType utledStartpunktMotOriginalBehandling(BehandlingReferanse revurdering);
    StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, EndringsresultatDiff differanse);
}
