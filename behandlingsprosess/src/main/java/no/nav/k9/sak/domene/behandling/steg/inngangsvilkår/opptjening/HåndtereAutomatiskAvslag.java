package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;

public interface HåndtereAutomatiskAvslag {

    void håndter(Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode);
}
