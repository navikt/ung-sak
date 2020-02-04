package no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.felles;

import static java.util.Collections.singletonList;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.InngangsvilkårStegImpl;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.MapTilOpptjeningAktiviteter;
import no.nav.foreldrepenger.behandling.steg.inngangsvilkår.opptjening.RyddOpptjening;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitet;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.opptjening.OpptjeningsvilkårResultat;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;

public abstract class VurderOpptjeningsvilkårStegFelles extends InngangsvilkårStegImpl {

    protected static final VilkårType OPPTJENINGSVILKÅRET = VilkårType.OPPTJENINGSVILKÅRET;
    private static List<VilkårType> STØTTEDE_VILKÅR = singletonList(OPPTJENINGSVILKÅRET);

    private OpptjeningRepository opptjeningRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingRepositoryProvider repositoryProvider;

    protected VurderOpptjeningsvilkårStegFelles() {
        // CDI proxy
    }

    public VurderOpptjeningsvilkårStegFelles(BehandlingRepositoryProvider repositoryProvider,
                                             OpptjeningRepository opptjeningRepository,
                                             InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste,
                                             BehandlingStegType behandlingStegType) {
        super(repositoryProvider, inngangsvilkårFellesTjeneste, behandlingStegType);
        this.opptjeningRepository = opptjeningRepository;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.repositoryProvider = repositoryProvider;
    }

    @Override
    protected void utførtRegler(BehandlingskontrollKontekst kontekst, Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        if (vilkårErVurdert(regelResultat, periode.getFomDato(), periode.getTomDato())) {
            OpptjeningsvilkårResultat opres = getVilkårresultat(behandling, regelResultat, periode);
            MapTilOpptjeningAktiviteter mapper = new MapTilOpptjeningAktiviteter();
            List<OpptjeningAktivitet> aktiviteter = mapTilOpptjeningsaktiviteter(mapper, opres);
            opptjeningRepository.lagreOpptjeningResultat(behandling, opres.getResultatOpptjent(), aktiviteter);
        } else {
            // rydd bort tidligere aktiviteter
            opptjeningRepository.lagreOpptjeningResultat(behandling, null, Collections.emptyList());
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

    private boolean vilkårErVurdert(RegelResultat regelResultat, LocalDate fom, LocalDate tom) {
        final var berørtePerioder = regelResultat.getVilkårene()
            .getVilkårene()
            .stream()
            .filter(v -> v.getVilkårType().equals(OPPTJENINGSVILKÅRET))
            .map(Vilkår::getPerioder)
            .flatMap(Collection::stream)
            .filter(it -> it.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
            .collect(Collectors.toList());
        return berørtePerioder.stream().noneMatch(it -> it.getGjeldendeUtfall().equals(Utfall.IKKE_VURDERT));
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
