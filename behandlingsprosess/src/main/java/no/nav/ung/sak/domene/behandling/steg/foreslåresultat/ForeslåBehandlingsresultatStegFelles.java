package no.nav.ung.sak.domene.behandling.steg.foreslåresultat;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class ForeslåBehandlingsresultatStegFelles implements ForeslåBehandlingsresultatSteg {

    private BehandlingRepository behandlingRepository;
    private Instance<ForeslåBehandlingsresultatTjeneste> foreslåBehandlingsresultatTjeneste;

    private VilkårResultatRepository vilkårResultatRepository;

    protected ForeslåBehandlingsresultatStegFelles() {
        // for CDI proxy
    }

    public ForeslåBehandlingsresultatStegFelles(BehandlingRepositoryProvider repositoryProvider,
                                                @Any Instance<ForeslåBehandlingsresultatTjeneste> foreslåBehandlingsresultatTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.foreslåBehandlingsresultatTjeneste = foreslåBehandlingsresultatTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        precondition(behandling);
        var ref = BehandlingReferanse.fra(behandling);

        var tjeneste = FagsakYtelseTypeRef.Lookup.find(foreslåBehandlingsresultatTjeneste, ref.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Har ikke " + getClass().getSimpleName() + " for ytelse=" + ref.getFagsakYtelseType()));
        tjeneste.foreslåBehandlingsresultatType(ref, kontekst);

        postcondition(behandlingId);

        // Dette steget genererer ingen aksjonspunkter
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    protected void precondition(Behandling behandling) {

        var ugyldigResultat = BehandlingResultatType.kodeMap().values().stream().filter(r -> r.erHenleggelse()).collect(Collectors.toSet());
        var resultatType = behandling.getBehandlingResultatType();
        if (ugyldigResultat.contains(resultatType)) {
            throw new IllegalStateException(
                "Behandling " + behandling.getId() + " har ugyldig resultatType=" + resultatType + ", støtter ikke allerede henlagt behandling i Foreslå Behandlingsresultat");
        }

        if (BehandlingResultatType.getInnvilgetKoder().contains(resultatType) || Set.of(FagsakYtelseType.PSB, FagsakYtelseType.PPN, FagsakYtelseType.OLP, FagsakYtelseType.OMP).contains(behandling.getFagsakYtelseType())) {
            validerAtAlleVilkårErVurdert(behandling.getId());
        }
    }

    protected void postcondition(Long behandlingId) {
        var ugyldigResultat = Set.of(BehandlingResultatType.IKKE_FASTSATT);

        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var resultatType = behandling.getBehandlingResultatType();
        if (ugyldigResultat.contains(resultatType)) {
            throw new IllegalStateException(
                "Behandling " + behandling.getId() + " har ugyldig resultatType=" + resultatType + " etter Foreslå Behandlingsresultat, må fastsette type endring/innvilgelse/avslag");
        }
    }

    private void validerAtAlleVilkårErVurdert(Long behandlingId) {
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        vilkårene.getVilkårene().forEach(this::validerVilkår);
    }

    private void validerVilkår(Vilkår vilkår) {
        List<VilkårPeriode> ikkeVurdertePerioder = vilkår.getPerioder().stream()
            .filter((Predicate<? super VilkårPeriode>) at -> Utfall.IKKE_VURDERT.equals(at.getGjeldendeUtfall()))
            .toList();
        if (!ikkeVurdertePerioder.isEmpty()) {
            throw new IllegalStateException(
                "Vilkåret " + vilkår.getVilkårType() + " har en eller flere perioder som ikke er vurdert: " + ikkeVurdertePerioder);
        }
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (!BehandlingStegType.FORESLÅ_BEHANDLINGSRESULTAT.equals(tilSteg)) {
            var behandlingId = kontekst.getBehandlingId();
            Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
            behandling.setBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT);
            behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        }
    }
}
