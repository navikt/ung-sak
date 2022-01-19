package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.opptjening;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening.HåndtereAutomatiskAvslag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OmsorgspengerHåndtereAutomatiskAvslag implements HåndtereAutomatiskAvslag {


    @Inject
    OmsorgspengerHåndtereAutomatiskAvslag() {
        // CDI
    }

    @Override
    public void håndter(Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        regelResultat.getAksjonspunktDefinisjoner().add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET));
    }
}
