package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.opptjening;

import javax.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening.HåndtereAutomatiskAvslag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PleiepengerHåndtereAutomatiskAvslag implements HåndtereAutomatiskAvslag {

    @Override
    public void håndter(Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        regelResultat.getAksjonspunktDefinisjoner().add(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);
    }
}
