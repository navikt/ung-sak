package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Utfall;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.inngangsvilkaar.RegelResultat;
import no.nav.vedtak.konfig.Tid;

public abstract class InngangsvilkårStegImpl implements InngangsvilkårSteg {

    private VilkårResultatRepository vilkårResultatRepository;
    private BehandlingRepository behandlingRepository;
    private InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste;
    private BehandlingRepositoryProvider repositoryProvider;
    private BehandlingStegType behandlingStegType;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    public InngangsvilkårStegImpl(BehandlingRepositoryProvider repositoryProvider, InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste, BehandlingStegType behandlingStegType) {
        this.repositoryProvider = repositoryProvider;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.inngangsvilkårFellesTjeneste = inngangsvilkårFellesTjeneste;
        this.behandlingStegType = behandlingStegType;
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

        final var aksjonspunkter = new ArrayList<AksjonspunktDefinisjon>();
        // Kall regelmotor
        vilkårTyper.forEach(vilkår -> aksjonspunkter.addAll(vurderVilkårIPerioder(vilkår, behandling, kontekst)));

        // Returner behandlingsresultat
        return stegResultat(aksjonspunkter);
    }

    private List<AksjonspunktDefinisjon> vurderVilkårIPerioder(VilkårType vilkår, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        final var intervaller = perioderTilVurdering(kontekst.getBehandlingId());
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, inngangsvilkårFellesTjeneste.getSkjæringstidspunkter(behandling.getId()));
        RegelResultat regelResultat = inngangsvilkårFellesTjeneste.vurderInngangsvilkår(Set.of(vilkår), behandling, ref, intervaller);

        // Oppdater behandling
        vilkårResultatRepository.lagre(kontekst.getBehandlingId(), regelResultat.getVilkårene());

        for (DatoIntervallEntitet intervall : intervaller) {
            utførtRegler(kontekst, behandling, regelResultat, intervall);
        }

        return regelResultat.getAksjonspunktDefinisjoner();
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    private boolean harAvslåttForrigeBehandling(Behandling revurdering) {
        Optional<Behandling> originalBehandling = revurdering.getOriginalBehandling();
        if (originalBehandling.isPresent()) {
            Behandlingsresultat behandlingsresultat = getBehandlingsresultat(originalBehandling.get());
            // Dersom originalBehandling er et beslutningsvedtak må vi lete videre etter det faktiske resultatet for å kunne vurdere om forrige behandling var avslått
            if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingsresultat.getBehandlingResultatType())) {
                return harAvslåttForrigeBehandling(originalBehandling.get());
            } else {
                return behandlingsresultat.isBehandlingsresultatAvslått();
            }
        }
        return false;
    }

    private boolean harÅpentOverstyringspunktForInneværendeSteg(Behandling behandling) {
        return behandling.getÅpneAksjonspunkter().stream()
            .filter(aksjonspunkt -> aksjonspunkt.getAksjonspunktDefinisjon().getAksjonspunktType().equals(AksjonspunktType.OVERSTYRING))
            .anyMatch(aksjonspunkt ->
                aksjonspunkt.getAksjonspunktDefinisjon().getBehandlingSteg().equals(behandlingStegType));
    }

    protected BehandleStegResultat stegResultat(List<AksjonspunktDefinisjon> aksjonspunkter) {
        return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunkter);
    }

    @SuppressWarnings("unused")
    protected void utførtRegler(BehandlingskontrollKontekst kontekst, Behandling behandling, RegelResultat regelResultat, DatoIntervallEntitet periode) {
        // template method
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType hoppesTilSteg,
                                   BehandlingStegType hoppesFraSteg) {
        if (!erVilkårOverstyrt(kontekst.getBehandlingId(), Tid.TIDENES_BEGYNNELSE, Tid.TIDENES_ENDE)) { // Fixme (k9) - Periodene som er knyttet til søknaden
            Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
            RyddVilkårTyper ryddVilkårTyper = new RyddVilkårTyper(modell, repositoryProvider, behandling, kontekst);
            ryddVilkårTyper.ryddVedTilbakeføring(vilkårHåndtertAvSteg());
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        }
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
            return vilkårene.getVilkårene()
                .stream()
                .filter(vilkår -> vilkårHåndtertAvSteg().contains(vilkår.getVilkårType()))
                .map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .filter(it -> it.getPeriode().overlapper(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))) // FIXME (k9) : Er det rett med overlapper her? Bør kanskje hente ut perioden direkte med fom tom som key
                .anyMatch(VilkårPeriode::getErOverstyrt);
        }
        return false;
    }

    @Override
    public List<DatoIntervallEntitet> perioderTilVurdering(Long behandlingId) {
        Optional<Vilkårene> resultatOpt = vilkårResultatRepository.hentHvisEksisterer(behandlingId);
        if (resultatOpt.isPresent()) {
            Vilkårene vilkårene = resultatOpt.get();
            return vilkårene.getVilkårene()
                .stream()
                .filter(vilkår -> vilkårHåndtertAvSteg().contains(vilkår.getVilkårType()))
                .map(Vilkår::getPerioder)
                .flatMap(Collection::stream)
                .filter(vp -> Utfall.IKKE_VURDERT.equals(vp.getGjeldendeUtfall()))
                .filter(vp -> !erVilkårOverstyrt(behandlingId, vp.getPeriode().getFomDato(), vp.getPeriode().getTomDato()))
                .map(VilkårPeriode::getPeriode)
                .collect(Collectors.toList());
        }
        return List.of();
    }

}
