package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import static java.util.Collections.singletonList;

import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.Instance;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.InngangsvilkårStegImpl;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;
import no.nav.k9.sak.inngangsvilkår.opptjening.regelmodell.OpptjeningsvilkårResultat;

public abstract class VurderOpptjeningsvilkårStegFelles extends InngangsvilkårStegImpl {

    protected static final VilkårType OPPTJENINGSVILKÅRET = VilkårType.OPPTJENINGSVILKÅRET;
    private static List<VilkårType> STØTTEDE_VILKÅR = singletonList(OPPTJENINGSVILKÅRET);
    protected BehandlingRepositoryProvider repositoryProvider;
    private Instance<HåndtereAutomatiskAvslag> automatiskAvslagHåndterer;
    private OpptjeningRepository opptjeningRepository;
    private BehandlingRepository behandlingRepository;

    protected VurderOpptjeningsvilkårStegFelles() {
        // CDI proxy
    }

    public VurderOpptjeningsvilkårStegFelles(BehandlingRepositoryProvider repositoryProvider,
                                             OpptjeningRepository opptjeningRepository,
                                             InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste,
                                             BehandlingStegType behandlingStegType,
                                             Instance<HåndtereAutomatiskAvslag> automatiskAvslagHåndterer) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, behandlingStegType);
        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.repositoryProvider = repositoryProvider;
        this.automatiskAvslagHåndterer = automatiskAvslagHåndterer;
    }

    @Override
    protected void utførtRegler(BehandlingskontrollKontekst kontekst, Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        if (regelResultat.vilkårErVurdert(periode.getFomDato(), periode.getTomDato(), VilkårType.OPPTJENINGSVILKÅRET)) {
            OpptjeningsvilkårResultat opres = getVilkårresultat(behandling, regelResultat, periode);
            MapTilOpptjeningAktiviteter mapper = new MapTilOpptjeningAktiviteter();
            List<OpptjeningAktivitet> aktiviteter = mapTilOpptjeningsaktiviteter(mapper, opres);
            opptjeningRepository.lagreOpptjeningResultat(behandling, periode.getFomDato(), opres.getResultatOpptjent(), aktiviteter);

            håndtereAutomatiskAvslag(behandling, regelResultat, periode);
        } else if (!erVilkårOverstyrt(kontekst.getBehandlingId(), periode.getFomDato(), periode.getTomDato())) {
            // rydd bort tidligere aktiviteter
            opptjeningRepository.lagreOpptjeningResultat(behandling, periode.getFomDato(), null, Collections.emptyList());
        }
    }

    private void håndtereAutomatiskAvslag(Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        if (regelResultat.vilkårErIkkeOppfylt(periode.getFomDato(), periode.getTomDato(), VilkårType.OPPTJENINGSVILKÅRET)) {
            // Legger til aksjonspunspunkt for å håndtere eventuelle 8-47 innvilgelser
            var håndterer = FagsakYtelseTypeRef.Lookup.find(automatiskAvslagHåndterer, behandling.getFagsakYtelseType()).orElseThrow();
            håndterer.håndter(behandling, regelResultat, periode);
        }
    }

    protected abstract List<OpptjeningAktivitet> mapTilOpptjeningsaktiviteter(MapTilOpptjeningAktiviteter mapper, OpptjeningsvilkårResultat oppResultat);

    private OpptjeningsvilkårResultat getVilkårresultat(Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        OpptjeningsvilkårResultat op = (OpptjeningsvilkårResultat) regelResultat.getEkstraResultaterPerPeriode()
            .get(OPPTJENINGSVILKÅRET)
            .get(periode);
        if (op == null) {
            throw new IllegalArgumentException(
                "Utvikler-feil: finner ikke resultat fra evaluering av Inngangsvilkår/Opptjeningsvilkåret:" + behandling.getId());
        }
        return op;
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType hoppesTilSteg, BehandlingStegType hoppesFraSteg) {
        super.vedHoppOverBakover(kontekst, modell, hoppesTilSteg, hoppesFraSteg);
        final var perioder = inngangsvilkårFellesTjeneste.utledPerioderTilVurdering(kontekst.getBehandlingId(), OPPTJENINGSVILKÅRET);
        perioder.forEach(periode -> {
            if (!erVilkårOverstyrt(kontekst.getBehandlingId(), periode.getFomDato(), periode.getTomDato())) {
                new RyddOpptjening(behandlingRepository, opptjeningRepository, repositoryProvider.getVilkårResultatRepository(), kontekst).ryddOppAktiviteter(periode.getFomDato(), periode.getTomDato());
            }
        });
    }

    @Override
    public List<VilkårType> vilkårHåndtertAvSteg() {
        return STØTTEDE_VILKÅR;
    }
}
