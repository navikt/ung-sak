package no.nav.k9.sak.domene.behandling.steg;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.inngangsvilkår.perioder.VilkårsPerioderTilVurderingTjeneste;

@BehandlingStegRef(kode = "START")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class StartSteg implements BehandlingSteg {

    public static final int PLEIEPENGER_VILKÅR_MELLOMLIGGENDE_PERIODE = 7;
    public static final int DEFAULT_MAKS_AVSTAND = 0;
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste;

    StartSteg() {
        // for CDI proxy
    }

    @Inject
    public StartSteg(BehandlingRepository behandlingRepository,
                     VilkårResultatRepository vilkårResultatRepository,
                     VilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.perioderTilVurderingTjeneste = perioderTilVurderingTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        // Utleder vilkår med en gang
        utledVilkår(kontekst);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void utledVilkår(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        opprettVilkår(behandling);
    }

    private void opprettVilkår(Behandling behandling) {
        // Opprett Vilkårsresultat med vilkårne som som skal vurderes, og sett dem som ikke vurdert
        var eksisterendeVilkår = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());
        VilkårResultatBuilder vilkårBuilder = Vilkårene.builderFraEksisterende(eksisterendeVilkår.orElse(null));
        var vilkårPeriodeMap = perioderTilVurderingTjeneste.utled(behandling.getId());
        var utledetAvstand = utledAvstand(behandling.getFagsakYtelseType());
        vilkårPeriodeMap.forEach((key, value) -> vilkårBuilder
            .medMaksMellomliggendePeriodeAvstand(utledetAvstand)
            .leggTilIkkeVurderteVilkår(new ArrayList<>(value), List.of(key)));
        var vilkårResultat = vilkårBuilder.build();
        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultat);
    }

    private int utledAvstand(FagsakYtelseType fagsakYtelseType) {
        if (FagsakYtelseType.PSB.equals(fagsakYtelseType)) {
            return PLEIEPENGER_VILKÅR_MELLOMLIGGENDE_PERIODE;
        }
        return DEFAULT_MAKS_AVSTAND;
    }

}
