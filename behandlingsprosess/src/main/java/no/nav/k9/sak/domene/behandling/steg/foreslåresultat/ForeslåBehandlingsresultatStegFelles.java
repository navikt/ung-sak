package no.nav.k9.sak.domene.behandling.steg.foreslåresultat;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public abstract class ForeslåBehandlingsresultatStegFelles implements ForeslåBehandlingsresultatSteg {

    private static final Logger logger = LoggerFactory.getLogger(ForeslåBehandlingsresultatStegFelles.class);

    private BehandlingRepository behandlingRepository;
    private Instance<ForeslåBehandlingsresultatTjeneste> foreslåBehandlingsresultatTjeneste;

    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    protected ForeslåBehandlingsresultatStegFelles() {
        // for CDI proxy
    }

    public ForeslåBehandlingsresultatStegFelles(BehandlingRepositoryProvider repositoryProvider,
                                                @Any Instance<ForeslåBehandlingsresultatTjeneste> foreslåBehandlingsresultatTjeneste,
                                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.foreslåBehandlingsresultatTjeneste = foreslåBehandlingsresultatTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        logger.info("Foreslår behandlingsresultat for behandling {}", ref);
        
        var tjeneste = FagsakYtelseTypeRef.Lookup.find(foreslåBehandlingsresultatTjeneste, ref.getFagsakYtelseType()).orElseThrow();
        tjeneste.foreslåVedtakVarsel(ref, kontekst);
        
        // TODO (Safir/OSS): Lagre Behandlingsresultat gjennom eget repository
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
        
        // Dette steget genererer ingen aksjonspunkter
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
