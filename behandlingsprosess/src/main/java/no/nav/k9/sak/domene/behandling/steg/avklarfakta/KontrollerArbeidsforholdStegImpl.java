package no.nav.k9.sak.domene.behandling.steg.avklarfakta;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingskontroll.StartpunktRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.registerinnhenting.KontrollerFaktaAksjonspunktUtleder;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "KOARB")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@StartpunktRef("KONTROLLER_ARBEIDSFORHOLD")
@ApplicationScoped
class KontrollerArbeidsforholdStegImpl implements KontrollerArbeidsforholdSteg {

    private KontrollerFaktaAksjonspunktUtleder tjeneste;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    KontrollerArbeidsforholdStegImpl() {
        // for CDI proxy
    }

    @Inject
    KontrollerArbeidsforholdStegImpl(BehandlingRepository behandlingRepository,
                                     SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                     @StartpunktRef("KONTROLLER_ARBEIDSFORHOLD") KontrollerArbeidsforholdTjenesteImpl tjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.tjeneste = tjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        List<AksjonspunktResultat> aksjonspunktResultat = tjeneste.utledAksjonspunkter(ref);
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultat);
    }
}
