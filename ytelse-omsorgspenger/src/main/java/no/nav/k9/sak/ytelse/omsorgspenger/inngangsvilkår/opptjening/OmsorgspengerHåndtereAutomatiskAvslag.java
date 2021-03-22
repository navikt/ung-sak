package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.opptjening;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening.HåndtereAutomatiskAvslag;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;
import no.nav.k9.sak.perioder.KravDokumentType;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.søknadsfrist.OMPVurderSøknadsfristTjeneste;

@ApplicationScoped
@FagsakYtelseTypeRef("OMP")
public class OmsorgspengerHåndtereAutomatiskAvslag implements HåndtereAutomatiskAvslag {

    private OMPVurderSøknadsfristTjeneste vurderSøknadsfristTjeneste;

    OmsorgspengerHåndtereAutomatiskAvslag() {
        // CDI
    }

    @Inject
    public OmsorgspengerHåndtereAutomatiskAvslag(@FagsakYtelseTypeRef("OMP") OMPVurderSøknadsfristTjeneste vurderSøknadsfristTjeneste) {
        this.vurderSøknadsfristTjeneste = vurderSøknadsfristTjeneste;
    }

    @Override
    public void håndter(Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        if (utbetalingTilBrukerIPerioden(behandling, periode)) {
            regelResultat.getAksjonspunktDefinisjoner().add(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET));
        }
    }

    private boolean utbetalingTilBrukerIPerioden(Behandling behandling, DatoIntervallEntitet periode) {
        var kravDokumentListMap = vurderSøknadsfristTjeneste.vurderSøknadsfrist(BehandlingReferanse.fra(behandling));

        var relevanteDokumenter = kravDokumentListMap.entrySet()
            .stream()
            .filter(it -> KravDokumentType.SØKNAD.equals(it.getKey().getType())) // Søknaden er alltid fra bruker
            .filter(it -> it.getValue()
                .stream()
                .filter(at -> Utfall.OPPFYLT.equals(at.getUtfall()))
                .anyMatch(at -> at.getPeriode().overlapper(periode)))
            .collect(Collectors.toSet());

        return !relevanteDokumenter.isEmpty();
    }
}
