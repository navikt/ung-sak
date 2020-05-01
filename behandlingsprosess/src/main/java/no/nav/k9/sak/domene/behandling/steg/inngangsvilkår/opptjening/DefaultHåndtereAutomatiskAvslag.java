package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;

@ApplicationScoped
@FagsakYtelseTypeRef
public class DefaultHåndtereAutomatiskAvslag implements HåndtereAutomatiskAvslag {

    @Override
    public void håndter(Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        // DO NOTHING
    }
}
