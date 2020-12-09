package no.nav.k9.sak.domene.behandling.steg.søknadsfrist;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.*;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.perioder.SøknadsfristTjeneste;
import no.nav.k9.sak.perioder.VurderSøknadsfristTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@BehandlingStegRef(kode = "VURDER_SØKNADSFRIST")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderSøknadsfristSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private Instance<SøknadsfristTjeneste> søknadsfristTjenester;
    private Instance<VurderSøknadsfristTjeneste> vurderSøknadsfristTjenester;
    private boolean vurderSøknadsfrist;

    VurderSøknadsfristSteg() {
        // CDI
    }

    @Inject
    public VurderSøknadsfristSteg(BehandlingRepository behandlingRepository,
                                  @Any Instance<SøknadsfristTjeneste> søknadsfristTjenester,
                                  @Any Instance<VurderSøknadsfristTjeneste> vurderSøknadsfristTjenester,
                                  @KonfigVerdi(value = "VURDER_SØKNADSFRIST", required = false, defaultVerdi = "false") boolean vurderSøknadsfrist) {
        this.behandlingRepository = behandlingRepository;
        this.søknadsfristTjenester = søknadsfristTjenester;
        this.vurderSøknadsfristTjenester = vurderSøknadsfristTjenester;
        this.vurderSøknadsfrist = vurderSøknadsfrist;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        if (vurderSøknadsfrist) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

            var periodeTjeneste = hentUtSøknadsperioderTjeneste(behandling);
            var tjeneste = hentVurderingsTjeneste(behandling);
            var referanse = BehandlingReferanse.fra(behandling);
            // Henter søkte perioder
            var søktePerioder = periodeTjeneste.hentPerioderFor(referanse);
            var vurdertePerioder = tjeneste.vurderSøknadsfrist(søktePerioder);
            // Henter tidligere vurderinger av fristavbrytende kontakt
            // TODO: Vurder (nye) dokumenter/perioder

            // TODO: Trengs det manuell vurdering?
            // TODO: Lagre vurdering
            // Detaljer hvertfall
            // TODO: Lagre vilkår?
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private VurderSøknadsfristTjeneste hentVurderingsTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VurderSøknadsfristTjeneste.class, vurderSøknadsfristTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VurderSøknadsfristTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }

    private SøknadsfristTjeneste hentUtSøknadsperioderTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(SøknadsfristTjeneste.class, søknadsfristTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("SøknadsfristTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }
}
