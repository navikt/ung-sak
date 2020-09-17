package no.nav.k9.sak.domene.behandling.steg.foreslåresultat;

import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public abstract class ForeslåBehandlingsresultatStegFelles implements ForeslåBehandlingsresultatSteg {

    private BehandlingRepository behandlingRepository;
    private Instance<ForeslåBehandlingsresultatTjeneste> foreslåBehandlingsresultatTjeneste;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private VilkårResultatRepository vilkårResultatRepository;

    protected ForeslåBehandlingsresultatStegFelles() {
        // for CDI proxy
    }

    public ForeslåBehandlingsresultatStegFelles(BehandlingRepositoryProvider repositoryProvider,
                                                @Any Instance<ForeslåBehandlingsresultatTjeneste> foreslåBehandlingsresultatTjeneste,
                                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårResultatRepository = repositoryProvider.getVilkårResultatRepository();
        this.foreslåBehandlingsresultatTjeneste = foreslåBehandlingsresultatTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());

        precondition(behandling);

        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);

        var tjeneste = FagsakYtelseTypeRef.Lookup.find(foreslåBehandlingsresultatTjeneste, ref.getFagsakYtelseType()).orElseThrow();
        tjeneste.foreslåBehandlingsresultatType(ref, kontekst);

        postcondition(behandling);

        // Dette steget genererer ingen aksjonspunkter
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    protected void precondition(Behandling behandling) {
        validerAtAlleVilkårErVurdert(behandling.getId());

        var ugyldigResultat = BehandlingResultatType.kodeMap().values().stream().filter(r -> r.erHenleggelse()).collect(Collectors.toSet());
        var resultatType = behandling.getBehandlingResultatType();
        if (ugyldigResultat.contains(resultatType)) {
            throw new IllegalStateException("Behandling "+ behandling.getId()+ " har ugyldig resultatType=" + resultatType + ", støtter ikke allerede henlagt behandling i Foreslå Behandlingsresultat");
        }
    }

    protected void postcondition(Behandling behandling) {
        var ugyldigResultat = Set.of(BehandlingResultatType.IKKE_FASTSATT);
        var resultatType = behandling.getBehandlingResultatType();
        if (ugyldigResultat.contains(resultatType)) {
            throw new IllegalStateException("Behandling "+ behandling.getId()+ " har ugyldig resultatType=" + resultatType + " etter Foreslå Behandlingsresultat, må fastsette type endring/innvilgelse/avslag");
        }
    }

    private void validerAtAlleVilkårErVurdert(Long behandlingId) {
        var vilkårene = vilkårResultatRepository.hent(behandlingId);

        vilkårene.getVilkårene().forEach(this::validerVilkår);
    }

    private void validerVilkår(Vilkår vilkår) {
        if (vilkår.getPerioder().stream().anyMatch(at -> Utfall.IKKE_VURDERT.equals(at.getGjeldendeUtfall()))) {
            throw new IllegalStateException("Vilkåret " + vilkår.getVilkårType() + " har en eller flere perioder som ikke er vurdert.");
        }
    }
}
