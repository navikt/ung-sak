package no.nav.ung.sak.domene.behandling.steg.innhentsaksopplysninger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kompletthet.KompletthetModell;
import no.nav.ung.sak.kompletthet.KompletthetResultat;

import java.util.List;

import static java.util.Collections.singletonList;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.INREG_AVSL;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING;
import static no.nav.ung.sak.behandlingskontroll.AksjonspunktResultat.opprettForAksjonspunkt;
import static no.nav.ung.sak.domene.behandling.steg.kompletthet.VurderKompletthetStegFelles.autopunktAlleredeUtført;

@BehandlingStegRef(value = INREG_AVSL)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class InnhentRegisteropplysningerResterendeOppgaverStegImpl implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private KompletthetModell kompletthetModell;

    InnhentRegisteropplysningerResterendeOppgaverStegImpl() {
        // for CDI proxy
    }

    @Inject
    public InnhentRegisteropplysningerResterendeOppgaverStegImpl(BehandlingRepository behandlingRepository,
                                                                 KompletthetModell kompletthetModell) {

        this.behandlingRepository = behandlingRepository;
        this.kompletthetModell = kompletthetModell;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        KompletthetResultat etterlysIM = !autopunktAlleredeUtført(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, behandling) ? kompletthetModell.vurderKompletthet(ref, List.of(AUTO_VENT_ETTERLYST_INNTEKTSMELDING)) : KompletthetResultat.oppfylt();
        if (!etterlysIM.erOppfylt()) {
            // Dette autopunktet har tilbakehopp/gjenopptak. Går ut av steget hvis auto utført før frist (manuelt av vent). Utført på/etter frist antas
            // automatisk gjenopptak.
            if (!etterlysIM.erFristUtløpt() && !autopunktAlleredeUtført(AUTO_VENT_ETTERLYST_INNTEKTSMELDING, behandling)) {
                return BehandleStegResultat.utførtMedAksjonspunktResultater(singletonList(opprettForAksjonspunkt(AUTO_VENT_ETTERLYST_INNTEKTSMELDING)));
            }
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();

    }
}
