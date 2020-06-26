package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.RegelResultat;

public abstract class InngangsvilkårStegImpl implements InngangsvilkårSteg {

    protected InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private BehandlingRepositoryProvider repositoryProvider;

    public InngangsvilkårStegImpl(BehandlingRepositoryProvider repositoryProvider, InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste, @SuppressWarnings("unused") BehandlingStegType behandlingStegType) {
        this.repositoryProvider = repositoryProvider;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.inngangsvilkårFellesTjeneste = inngangsvilkårFellesTjeneste;
    }

    protected InngangsvilkårStegImpl() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // Hent behandlingsgrunnlag og vilkårtyper
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        List<VilkårType> vilkårHåndtertAvSteg = vilkårHåndtertAvSteg();
        Set<VilkårType> vilkårTyper = vilkårResultatRepository.hent(kontekst.getBehandlingId())
            .getVilkårene()
            .stream()
            .map(Vilkår::getVilkårType)
            .filter(vilkårHåndtertAvSteg::contains)
            .collect(Collectors.toSet());

        if (!(vilkårHåndtertAvSteg.isEmpty() || !vilkårTyper.isEmpty())) {
            throw new IllegalArgumentException(String.format("Utviklerfeil: Steg[%s] håndterer ikke angitte vilkår %s", this.getClass(), vilkårTyper)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        final var aksjonspunkter = new ArrayList<AksjonspunktResultat>();
        // Kall regelmotor
        vilkårTyper.forEach(vilkår -> aksjonspunkter.addAll(vurderVilkårIPerioder(vilkår, behandling, kontekst)));

        // Returner behandlingsresultat
        return stegResultat(aksjonspunkter);
    }

    private List<AksjonspunktResultat> vurderVilkårIPerioder(VilkårType vilkår, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var intervaller = perioderTilVurdering(kontekst.getBehandlingId(), vilkår);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, inngangsvilkårFellesTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId()));
        RegelResultat regelResultat = inngangsvilkårFellesTjeneste.vurderInngangsvilkår(Set.of(vilkår), ref, intervaller);

        // Oppdater behandling
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), regelResultat.getVilkårene());

        for (DatoIntervallEntitet intervall : intervaller) {
            utførtRegler(kontekst, behandling, regelResultat, intervall);
        }

        return new ArrayList<>(regelResultat.getAksjonspunktDefinisjoner());
    }

    protected BehandleStegResultat stegResultat(List<AksjonspunktResultat> aksjonspunkter) {
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
    }

    @SuppressWarnings("unused")
    protected void utførtRegler(BehandlingskontrollKontekst kontekst, Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        // template method
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType hoppesTilSteg,
                                   BehandlingStegType hoppesFraSteg) {
        vilkårHåndtertAvSteg().forEach(vilkår -> håndterHoppOverBakoverFor(vilkår, kontekst, modell));
    }

    private void håndterHoppOverBakoverFor(VilkårType vilkår, BehandlingskontrollKontekst kontekst, BehandlingStegModell modell) {
        final var perioder = inngangsvilkårFellesTjeneste.utledPerioderTilVurdering(kontekst.getBehandlingId(), vilkår);
        perioder.forEach(periode -> {
            if (!erVilkårOverstyrt(kontekst.getBehandlingId(), periode.getFomDato(), periode.getTomDato())) {
                ryddOppVilkårsPeriode(kontekst, modell, periode);
            }
        });
    }

    protected void ryddOppVilkårsPeriode(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, DatoIntervallEntitet periode) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        RyddVilkårTyper ryddVilkårTyper = new RyddVilkårTyper(modell, repositoryProvider, behandling, kontekst);
        ryddVilkårTyper.ryddVedTilbakeføring();
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    @Override
    public void vedHoppOverFramover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType hoppesFraSteg,
                                    BehandlingStegType hoppesTilSteg) {
        // TODO (k9) : skal det hoppes fremover?
    }

    protected boolean erVilkårOverstyrt(Long behandlingId, LocalDate fom, LocalDate tom) {
        Optional<Vilkårene> resultatOpt = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (resultatOpt.isPresent()) {
            Vilkårene vilkårene = resultatOpt.get();
            var periode = vilkårene.getVilkårene()
                .stream()
                .filter(vilkår -> vilkårHåndtertAvSteg().contains(vilkår.getVilkårType()))
                .map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .filter(it -> it.getPeriode().equals(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)))
                .collect(Collectors.toSet());
            if (periode.size() == 0) {
                return false;
            } else if (periode.size() == 1) {
                return periode.iterator().next().getErOverstyrt();
            } else {
                throw new IllegalStateException("Fant flere vilkårsperioder som er like med søkt periode..");
            }
        }
        return false;
    }

    @Override
    public NavigableSet<DatoIntervallEntitet> perioderTilVurdering(Long behandlingId, VilkårType vilkårType) {
        return inngangsvilkårFellesTjeneste.utledPerioderTilVurdering(behandlingId, vilkårType);
    }

}
