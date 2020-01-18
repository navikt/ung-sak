package no.nav.foreldrepenger.behandling.steg.avklarfakta.fp;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.KontrollerFaktaSteg;
import no.nav.foreldrepenger.behandling.steg.avklarfakta.RyddRegisterData;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.StartpunktRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.impl.DefaultVilkårUtleder;
import no.nav.foreldrepenger.inngangsvilkaar.impl.UtledeteVilkår;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "KOFAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@StartpunktRef
@ApplicationScoped
class KontrollerFaktaStegImpl implements KontrollerFaktaSteg {

    private KontrollerFaktaTjeneste tjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    KontrollerFaktaStegImpl() {
        // for CDI proxy
    }

    @Inject
    KontrollerFaktaStegImpl(BehandlingRepositoryProvider repositoryProvider,
                            SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                            @FagsakYtelseTypeRef KontrollerFaktaTjeneste tjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.repositoryProvider = repositoryProvider;
        this.tjeneste = tjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        List<AksjonspunktResultat> aksjonspunktResultater = tjeneste.utledAksjonspunkter(ref);
        utledVilkår(kontekst);
        behandling.setStartpunkt(StartpunktType.INNGANGSVILKÅR_OPPLYSNINGSPLIKT); // Settes til første steg.
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultater);
    }

    private void utledVilkår(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        UtledeteVilkår utledeteVilkår = new DefaultVilkårUtleder().utledVilkår(behandling);
        opprettVilkår(utledeteVilkår, behandling, kontekst.getSkriveLås());
    }

    private void opprettVilkår(UtledeteVilkår utledeteVilkår, Behandling behandling, BehandlingLås skriveLås) {
        // Opprett Vilkårsresultat med vilkårne som som skal vurderes, og sett dem som ikke vurdert
        VilkårResultatBuilder vilkårBuilder = getBehandlingsresultat(behandling) != null
            ? VilkårResultat.builderFraEksisterende(getBehandlingsresultat(behandling).getVilkårResultat())
            : VilkårResultat.builder();
        final var vilkårResultat = vilkårBuilder.leggTilIkkeVurderteVilkår(utledPerioderTilVurdering(behandling.getId()), utledeteVilkår.getAlleAvklarte()).build();
        getBehandlingsresultat(behandling).medOppdatertVilkårResultat(vilkårResultat);
        behandlingRepository.lagre(getBehandlingsresultat(behandling).getVilkårResultat(), skriveLås);
    }

    private List<DatoIntervallEntitet> utledPerioderTilVurdering(Long behandlingId) {
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        return List.of(DatoIntervallEntitet.fraOgMed(skjæringstidspunkter.getUtledetSkjæringstidspunkt())); // FIXME (k9) - Søknadsperioder som skal vurderes
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!BehandlingStegType.KONTROLLER_FAKTA.equals(fraSteg)) {
            RyddRegisterData rydder = new RyddRegisterData(repositoryProvider, kontekst);
            rydder.ryddRegisterdata();
        }
    }
}
