package no.nav.k9.sak.domene.behandling.steg;

import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.konfig.KonfigVerdi;

@BehandlingStegRef(kode = "INIT_VILKÅR")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class InitierVilkårSteg implements BehandlingSteg {

    private Set<Saksnummer> feilendSaker = Set.of(new Saksnummer("6L5CG"), new Saksnummer("6JPNC"));
    private BehandlingRepository behandlingRepository;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingProsesseringTjeneste prosesseringTjeneste;
    private Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester;
    private boolean valideringDeaktivert;

    InitierVilkårSteg() {
        // for CDI proxy
    }

    @Inject
    public InitierVilkårSteg(BehandlingRepository behandlingRepository,
                             VilkårResultatRepository vilkårResultatRepository,
                             BehandlingProsesseringTjeneste prosesseringTjeneste,
                             @Any Instance<VilkårsPerioderTilVurderingTjeneste> vilkårsPerioderTilVurderingTjenester,
                             @KonfigVerdi(value = "VILKAR_FAGSAKPERIODE_VALIDERING_DEAKTIVERT", required = false) boolean valideringDeaktivert) {
        this.behandlingRepository = behandlingRepository;
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.prosesseringTjeneste = prosesseringTjeneste;
        this.vilkårsPerioderTilVurderingTjenester = vilkårsPerioderTilVurderingTjenester;
        this.valideringDeaktivert = valideringDeaktivert;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        // Utleder vilkår med en gang
        try {
            utledVilkår(behandling);
        } catch (NoSuchElementException e) {
            // FIXME: Rydd opp, midlertidig tiltak pga behandlinger i limbo
            if (FagsakYtelseType.OMSORGSPENGER.equals(behandling.getFagsakYtelseType()) && feilendSaker.contains(behandling.getFagsak().getSaksnummer())) {
                // Planlegg ny fortsett behandling
                prosesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
                return BehandleStegResultat.tilbakeførtTilSteg(BehandlingStegType.INIT_PERIODER);
            }
        }
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
            vilkårBuilder.medBoundry(behandling.getFagsak().getPeriode());
        }

        var perioderTilVurderingTjeneste = FagsakYtelseTypeRef.Lookup.find(vilkårsPerioderTilVurderingTjenester, behandling.getFagsakYtelseType()).orElseThrow();
        var utledetAvstand = perioderTilVurderingTjeneste.maksMellomliggendePeriodeAvstand();
        var fullstendigePerioder = perioderTilVurderingTjeneste.utledFullstendigePerioder(behandling.getId());
        var fullstendigTidslinje = fullstendigTidslinje(utledetAvstand, perioderTilVurderingTjeneste.getKantIKantVurderer(), fullstendigePerioder);
        var vilkårPeriodeMap = perioderTilVurderingTjeneste.utled(behandling.getId());
        var perioderSomSkalTilbakestilles = perioderTilVurderingTjeneste.perioderSomSkalTilbakestilles(behandling.getId());

        vilkårBuilder.medMaksMellomliggendePeriodeAvstand(utledetAvstand)
            .medFagsakTidslinje(fullstendigTidslinje)
            .medKantIKantVurderer(perioderTilVurderingTjeneste.getKantIKantVurderer())
            .leggTilIkkeVurderteVilkår(vilkårPeriodeMap, perioderSomSkalTilbakestilles);
        var vilkårResultat = vilkårBuilder.build();

        vilkårResultatRepository.lagre(behandling.getId(), vilkårResultat);
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
            vb.leggTil(vb.hentBuilderFor(datoIntervallEntitet));
        }
        return vb;
    }
}
