package no.nav.ung.sak.domene.behandling.steg;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

@BehandlingStegRef(value = BehandlingStegType.INIT_VILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class InitierVilkårSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;

    InitierVilkårSteg() {
        // for CDI proxy
    }

    @Inject
    public InitierVilkårSteg(BehandlingRepository behandlingRepository,
                             VilkårResultatRepository vilkårResultatRepository,
                             @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        // Utleder vilkår med en gang
        utledVilkår(behandling);
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void utledVilkår(Behandling behandling) {
        opprettVilkår(behandling);
    }

    private void opprettVilkår(Behandling behandling) {
        // Opprett Vilkårsresultat med vilkårne som som skal vurderes, og sett dem som ikke vurdert
        var eksisterendeVilkår = vilkårResultatRepository.hentHvisEksisterer(behandling.getId());
        VilkårResultatBuilder vilkårBuilder = Vilkårene.builderFraEksisterende(eksisterendeVilkår.orElse(null)).medBoundry(behandling.getFagsak().getPeriode(), true);

        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(behandling);
        int utledetAvstand = perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand();
        var vilkårPeriodeMap = perioderTilVurderingTjeneste.utledRådataTilUtledningAvVilkårsperioder(behandling.getId());
        vilkårBuilder.medMaksMellomliggendePeriodeAvstand(utledetAvstand)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .leggTilIkkeVurderteVilkår(vilkårPeriodeMap);
        var vilkårResultat = vilkårBuilder.build();

        validerResultat(vilkårResultat, vilkårPeriodeMap);

        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultat, behandling.getFagsak().getPeriode());
    }

    private void validerResultat(Vilkårene vilkårResultat, Map<VilkårType, NavigableSet<DatoIntervallEntitet>> vilkårPeriodeMap) {
        var vilkårene = vilkårResultat.getVilkårene().stream().map(Vilkår::getVilkårType).collect(Collectors.toSet());
        if (!vilkårene.containsAll(vilkårPeriodeMap.keySet())) {
            throw new IllegalStateException("Vilkårsresultat inneholder ikke alle forventede vilkårtyper: "
                + vilkårPeriodeMap.keySet()
                + ", vilkårResultat" + vilkårResultat);
        }
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }

}
