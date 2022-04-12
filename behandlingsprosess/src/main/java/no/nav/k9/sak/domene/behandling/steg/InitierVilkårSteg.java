package no.nav.k9.sak.domene.behandling.steg;

import java.util.Map;
import java.util.NavigableSet;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;

@BehandlingStegRef(value = BehandlingStegType.INIT_VILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class InitierVilkårSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private boolean valideringDeaktivert;

    InitierVilkårSteg() {
        // for CDI proxy
    }

    @Inject
    public InitierVilkårSteg(BehandlingRepository behandlingRepository,
                             VilkårResultatRepository vilkårResultatRepository,
                             @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                             @KonfigVerdi(value = "VILKAR_FAGSAKPERIODE_VALIDERING_DEAKTIVERT", required = false) boolean valideringDeaktivert) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.valideringDeaktivert = valideringDeaktivert;
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
        VilkårResultatBuilder vilkårBuilder = Vilkårene.builderFraEksisterende(eksisterendeVilkår.orElse(null));
        if (!valideringDeaktivert) {
            vilkårBuilder.medBoundry(behandling.getFagsak().getPeriode(), true);
        }

        var perioderTilVurderingTjeneste = getPerioderTilVurderingTjeneste(behandling);
        var utledetAvstand = perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand();
        var fullstendigePerioder = perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId());
        var fullstendigTidslinje = fullstendigTidslinje(utledetAvstand, perioderTilVurderingTjeneste.getKantIKantVurderer(), fullstendigePerioder);
        var vilkårPeriodeMap = perioderTilVurderingTjeneste.utledRådataTilUtledningAvVilkårsperioder(behandling.getId());
        var perioderSomSkalTilbakestilles = perioderTilVurderingTjeneste.perioderSomSkalTilbakestilles(behandling.getId());

        vilkårBuilder.medMaksMellomliggendePeriodeAvstand(utledetAvstand)
            .medFullstendigTidslinje(fullstendigTidslinje)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .leggTilIkkeVurderteVilkår(vilkårPeriodeMap, perioderSomSkalTilbakestilles);
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

    /**
     * Utleder tidslinje for hele fagsaken med vilkårsreglene
     *
     * @param utledetAvstand    avstand for mellomliggende perioder
     * @param kantIKantVurderer kant i kant vurderer
     * @param allePerioder      alle vilkårsperioder for fagsaken
     * @return dummy vilkårsbuilder for, null for ikke implemterte ytelser
     */
    private VilkårBuilder fullstendigTidslinje(int utledetAvstand, KantIKantVurderer kantIKantVurderer, NavigableSet<DatoIntervallEntitet> allePerioder) {
        if (allePerioder == null) {
            return null;
        }
        var vb = new VilkårBuilder()
            .somDummy()
            .medKantIKantVurderer(kantIKantVurderer)
            .medMaksMellomliggendePeriodeAvstand(utledetAvstand);
        for (DatoIntervallEntitet datoIntervallEntitet : allePerioder) {
            vb.leggTil(vb.hentBuilderFor(datoIntervallEntitet)
                .medUtfall(Utfall.IKKE_VURDERT));
        }
        return vb;
    }

    private VilkårsPerioderTilVurderingTjeneste getPerioderTilVurderingTjeneste(Behandling behandling) {
        return BehandlingTypeRef.Lookup.find(VilkårsPerioderTilVurderingTjeneste.class, vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType(), behandling.getType())
            .orElseThrow(() -> new UnsupportedOperationException("VilkårsPerioderTilVurderingTjeneste ikke implementert for ytelse [" + behandling.getFagsakYtelseType() + "], behandlingtype [" + behandling.getType() + "]"));
    }

}
