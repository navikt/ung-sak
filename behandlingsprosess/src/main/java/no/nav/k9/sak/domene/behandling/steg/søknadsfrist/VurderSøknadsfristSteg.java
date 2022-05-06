package no.nav.k9.sak.domene.behandling.steg.søknadsfrist;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_SØKNADSFRIST;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristRepository;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.AvklartSøknadsfristResultat;
import no.nav.k9.sak.behandlingslager.behandling.søknadsfrist.KravDokumentHolder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.perioder.SøknadsfristTjeneste;

@BehandlingStegRef(value = VURDER_SØKNADSFRIST)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderSøknadsfristSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private AksjonspunktKontrollRepository kontrollRepository;
    private Instance<SøknadsfristTjeneste> vurderSøknadsfristTjenester;
    private VilkårResultatRepository vilkårResultatRepository;
    private AvklartSøknadsfristRepository avklartSøknadsfristRepository;

    VurderSøknadsfristSteg() {
        // CDI
    }

    @Inject
    public VurderSøknadsfristSteg(BehandlingRepository behandlingRepository,
                                  VilkårResultatRepository vilkårResultatRepository,
                                  AksjonspunktKontrollRepository kontrollRepository,
                                  @Any Instance<SøknadsfristTjeneste> vurderSøknadsfristTjenester,
                                  AvklartSøknadsfristRepository avklartSøknadsfristRepository) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.kontrollRepository = kontrollRepository;
        this.vurderSøknadsfristTjenester = vurderSøknadsfristTjenester;
        this.avklartSøknadsfristRepository = avklartSøknadsfristRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var aksjonspunktFor = behandling.getAksjonspunktFor(AksjonspunktKodeDefinisjon.OVERSTYRING_AV_SØKNADSFRISTVILKÅRET_KODE);
        aksjonspunktFor.ifPresent(this::settÅpentAksjonspunktTilUtført);

        var vilkårene = vilkårResultatRepository.hentHvisEksisterer(kontekst.getBehandlingId());

        var tjeneste = hentVurderingsTjeneste(behandling);
        var referanse = BehandlingReferanse.fra(behandling);

        // Henter søkte perioder
        var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene.orElse(null));
        var resultatBuilder = tjeneste.vurderSøknadsfrist(referanse, vilkårResultatBuilder);

        Vilkårene oppdatertVilkår = resultatBuilder.build();
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), oppdatertVilkår, behandling.getFagsak().getPeriode());
        var avklartSøknadsfristResultatOpt = avklartSøknadsfristRepository.hentHvisEksisterer(kontekst.getBehandlingId());

        if (kreverManuellAvklaring(oppdatertVilkår.getVilkår(VilkårType.SØKNADSFRIST)) || erManuellRevurderingOgHarGjortVurderingerTidligere(behandling, avklartSøknadsfristResultatOpt)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean erManuellRevurderingOgHarGjortVurderingerTidligere(Behandling behandling, Optional<AvklartSøknadsfristResultat> avklartSøknadsfristResultatOpt) {
        return behandling.erManueltOpprettet() && harGjortAvklaringerTidligere(avklartSøknadsfristResultatOpt);
    }

    private boolean harGjortAvklaringerTidligere(Optional<AvklartSøknadsfristResultat> avklartSøknadsfristResultatOpt) {
        return avklartSøknadsfristResultatOpt.flatMap(AvklartSøknadsfristResultat::getAvklartHolder)
            .map(KravDokumentHolder::getDokumenter)
            .map(Set::isEmpty)
            .map(it -> !it).orElse(false);
    }

    private void settÅpentAksjonspunktTilUtført(no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt aksjonspunkt) {
        if (aksjonspunkt.erÅpentAksjonspunkt()) {
            kontrollRepository.setTilUtført(aksjonspunkt, aksjonspunkt.getBegrunnelse());
        }
    }

    private boolean kreverManuellAvklaring(Optional<Vilkår> vurdertePerioder) {
        if (vurdertePerioder.isEmpty()) {
            return false;
        }
        return vurdertePerioder.get().getPerioder().stream().anyMatch(it -> Utfall.IKKE_VURDERT.equals(it.getUtfall()));
    }

    private SøknadsfristTjeneste hentVurderingsTjeneste(Behandling behandling) {
        return FagsakYtelseTypeRef.Lookup.find(SøknadsfristTjeneste.class, vurderSøknadsfristTjenester, behandling.getFagsakYtelseType())
            .orElseThrow(() -> new UnsupportedOperationException("VurderSøknadsfristTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }
}
