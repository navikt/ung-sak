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
        List<Vilkår> vilkårene = vilkårResultatRepository.hent(kontekst.getBehandlingId()).getVilkårene();

        Set<VilkårType> alleVilkårTyper = vilkårene
            .stream()
            .map(Vilkår::getVilkårType)
            .collect(Collectors.toSet());

        Set<VilkårType> aktuelleVilkårTyper = alleVilkårTyper
            .stream()
            .filter(vilkårHåndtertAvSteg::contains)
            .collect(Collectors.toSet());

        if (!vilkårHåndtertAvSteg.isEmpty() && aktuelleVilkårTyper.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Utviklerfeil: Steg[%s] håndterer ikke angitte vilkår %s, håndterer kun [%s]. Alle vilkår tilgjengelig: %s", this.getClass(), vilkårHåndtertAvSteg, aktuelleVilkårTyper, //$NON-NLS-1$
                    alleVilkårTyper)); //$NON-NLS-2$
        }

        final var aksjonspunkter = new ArrayList<AksjonspunktResultat>();
        // Kall regelmotor
        aktuelleVilkårTyper.forEach(vilkår -> aksjonspunkter.addAll(vurderVilkårIPerioder(vilkår, behandling, kontekst)));

        // Returner behandlingsresultat
        return stegResultat(aksjonspunkter);
    }

    private List<AksjonspunktResultat> vurderVilkårIPerioder(VilkårType vilkår, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        var intervaller = perioderTilVurdering(kontekst.getBehandlingId(), vilkår);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        RegelResultat regelResultat = inngangsvilkårFellesTjeneste.vurderInngangsvilkår(Set.of(vilkår), ref, intervaller);

        // Oppdater behandling
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), regelResultat.getVilkårene());

        for (DatoIntervallEntitet intervall : intervaller) {
            utførtRegler(kontekst, behandling, regelResultat, intervall);
        }

        //
        if (inngangsvilkårFellesTjeneste.getEnableForlengelse() && behandling.getOriginalBehandlingId().isPresent()) {
            var forlengelserTilVurdering = inngangsvilkårFellesTjeneste.utledForlengelserTilVurdering(ref.getBehandlingId(), vilkår);

            if (!forlengelserTilVurdering.isEmpty()) {
                var vilkårene = vilkårResultatRepository.hent(behandling.getId());
                var vilkårResultatBuilder = Vilkårene.builderFraEksisterende(vilkårene);
                var vedtattUtfallPåVilkåret = vilkårResultatRepository.hentHvisEksisterer(behandling.getOriginalBehandlingId().orElseThrow())
                    .orElseThrow()
                    .getVilkår(vilkår)
                    .orElseThrow();

                var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(vilkår);

                for (DatoIntervallEntitet datoIntervallEntitet : forlengelserTilVurdering) {
                    var eksisteredeVurdering = vedtattUtfallPåVilkåret.finnPeriodeForSkjæringstidspunkt(datoIntervallEntitet.getFomDato());

                    var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(datoIntervallEntitet)
                        .forlengelseAv(eksisteredeVurdering);

                    vilkårBuilder.leggTil(vilkårPeriodeBuilder);

                    håndterForlengelse(kontekst, behandling, datoIntervallEntitet); // kopi av resultat modeller
                }
                vilkårResultatBuilder.leggTil(vilkårBuilder);

                vilkårResultatRepository.lagre(behandling.getId(), vilkårResultatBuilder.build());
            }

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

    @SuppressWarnings("unused")
    protected void håndterForlengelse(BehandlingskontrollKontekst kontekst, Behandling behandling, DatoIntervallEntitet periode) {
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
                tilbakestill(kontekst, modell, periode);
            }
        });
    }

    protected void tilbakestill(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, @SuppressWarnings("unused") DatoIntervallEntitet periode) {
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
