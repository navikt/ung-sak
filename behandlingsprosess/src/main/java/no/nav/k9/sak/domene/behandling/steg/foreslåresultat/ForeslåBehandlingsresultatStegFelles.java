package no.nav.k9.sak.domene.behandling.steg.foreslåresultat;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(ForeslåBehandlingsresultatStegFelles.class);

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

        validerAtAlleVilkårErVurdert(kontekst.getBehandlingId());

        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        logger.info("Foreslår behandlingsresultat for behandling {}", (ref != null) ? ref.getBehandlingUuid() : "MANGLER REF");

        var tjeneste = FagsakYtelseTypeRef.Lookup.find(foreslåBehandlingsresultatTjeneste, ref.getFagsakYtelseType()).orElseThrow();
        tjeneste.foreslåVedtakVarsel(ref, kontekst);

        // TODO (Safir/OSS): Lagre Behandlingsresultat gjennom eget repository
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());

        // Dette steget genererer ingen aksjonspunkter
        return BehandleStegResultat.utførtUtenAksjonspunkter();
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
