package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.opptjening;

import javax.enterprise.context.ApplicationScoped;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening.HåndtereAutomatiskAvslag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef("PSB")
public class PleiepengerHåndtereAutomatiskAvslag implements HåndtereAutomatiskAvslag {
    @Override
    public void håndter(Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode, List<OpptjeningAktivitet> opptjeningAktiviteter) {
        regelResultat.getAksjonspunktDefinisjoner().add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET));
    }

}
