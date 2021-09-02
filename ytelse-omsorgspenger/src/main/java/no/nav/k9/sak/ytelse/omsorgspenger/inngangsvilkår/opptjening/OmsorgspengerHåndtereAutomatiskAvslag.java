package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.opptjening;

import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
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
    private Boolean overstyringFjernet;

    OmsorgspengerHåndtereAutomatiskAvslag() {
        // CDI
    }

    @Inject
    public OmsorgspengerHåndtereAutomatiskAvslag(@FagsakYtelseTypeRef("OMP") OMPVurderSøknadsfristTjeneste vurderSøknadsfristTjeneste,
                                                 @KonfigVerdi(value = "OVERSTYRING_OPPTJ_AKT_FJERNET", defaultVerdi = "true") Boolean overstyringFjernet) {
        this.vurderSøknadsfristTjeneste = vurderSøknadsfristTjeneste;
        this.overstyringFjernet = overstyringFjernet;
    }

    @Override
    public void håndter(Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        if (overstyringFjernet || utbetalingTilBrukerIPerioden(behandling, periode)) {
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
