package no.nav.k9.sak.domene.behandling.steg.søknadsfrist;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.*;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.perioder.SøknadsfristTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@BehandlingStegRef(kode = "VURDER_SØKNADSFRIST")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderSøknadsfristSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private Instance<SøknadsfristTjeneste> vurderSøknadsfristTjenester;
    private VilkårResultatRepository vilkårResultatRepository;
    private boolean vurderSøknadsfrist;

    VurderSøknadsfristSteg() {
        // CDI
    }

    @Inject
    public VurderSøknadsfristSteg(BehandlingRepository behandlingRepository,
                                  VilkårResultatRepository vilkårResultatRepository,
                                  @Any Instance<SøknadsfristTjeneste> vurderSøknadsfristTjenester,
                                  @KonfigVerdi(value = "VURDER_SØKNADSFRIST", required = false, defaultVerdi = "false") boolean vurderSøknadsfrist) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vurderSøknadsfristTjenester = vurderSøknadsfristTjenester;
        this.vurderSøknadsfrist = vurderSøknadsfrist;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        if (vurderSøknadsfrist) {
            var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            var vilkårene = vilkårResultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());

            var tjeneste = hentVurderingsTjeneste(behandling);
            var referanse = BehandlingReferanse.fra(behandling);

            // Henter søkte perioder
            var resultatBuilder = tjeneste.vurderSøknadsfrist(referanse, vilkårene.map(Vilkårene::builderFraEksisterende).orElse(Vilkårene.builder()));

            Vilkårene oppdatertVilkår = resultatBuilder.build();
            vilkårResultatRepository.lagre(kontekst.getBehandlingId(), oppdatertVilkår);

            if (kreverManuellAvklaring(oppdatertVilkår.getVilkår(VilkårType.SØKNADSFRIST))) {
                // Legg til aksjonspunkt
                return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST));
            }

        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean kreverManuellAvklaring(Optional<Vilkår> vurdertePerioder) {
        if (vurdertePerioder.isEmpty()) {
            return false;
        }
        return vurdertePerioder.get().getPerioder().stream().anyMatch(it -> Utfall.IKKE_VURDERT.equals(it.getUtfall()));
    }

    private SøknadsfristTjeneste hentVurderingsTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(SøknadsfristTjeneste.class, vurderSøknadsfristTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VurderSøknadsfristTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }
}
