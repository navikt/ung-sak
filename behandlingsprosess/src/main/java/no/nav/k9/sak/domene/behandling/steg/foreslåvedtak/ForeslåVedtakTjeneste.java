package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.økonomi.tilbakekreving.samkjøring.SjekkTilbakekrevingAksjonspunktUtleder;

@ApplicationScoped
class ForeslåVedtakTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForeslåVedtakTjeneste.class);

    private SjekkMotAndreYtelserTjeneste sjekkMotAndreYtelserTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private Instance<ForeslåVedtakManueltUtleder> foreslåVedtakManueltUtledere;

    private SjekkTilbakekrevingAksjonspunktUtleder sjekkMotTilbakekrevingTjeneste;

    protected ForeslåVedtakTjeneste() {
        // CDI proxy
    }

    @Inject
    ForeslåVedtakTjeneste(FagsakRepository fagsakRepository,
                          BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                          SjekkMotAndreYtelserTjeneste sjekkMotAndreYtelserTjeneste,
                          SjekkTilbakekrevingAksjonspunktUtleder sjekkMotTilbakekrevingTjeneste,
                          @Any Instance<ForeslåVedtakManueltUtleder> foreslåVedtakManueltUtledere) {
        this.sjekkMotAndreYtelserTjeneste = sjekkMotAndreYtelserTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.foreslåVedtakManueltUtledere = foreslåVedtakManueltUtledere;
        this.sjekkMotTilbakekrevingTjeneste = sjekkMotTilbakekrevingTjeneste;
    }

    public BehandleStegResultat foreslåVedtak(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        long fagsakId = behandling.getFagsakId();
        Fagsak fagsak = fagsakRepository.finnEksaktFagsak(fagsakId);
        if (fagsak.getSkalTilInfotrygd()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new ArrayList<>();
        aksjonspunktDefinisjoner.addAll(sjekkMotAndreYtelserTjeneste.sjekkMotGsakOppgaverOgOverlappendeYtelser(behandling.getAktørId(), behandling));
        aksjonspunktDefinisjoner.addAll(sjekkMotTilbakekrevingTjeneste.sjekkMotÅpenIkkeoverlappendeTilbakekreving(behandling));

        Optional<Aksjonspunkt> vedtakUtenTotrinnskontroll = behandling
            .getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL);
        if (vedtakUtenTotrinnskontroll.isPresent()) {
            behandling.nullstillToTrinnsBehandling();
            return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjoner);
        }

        if (skalUtføreTotrinnsbehandling(behandling)) {
            håndterTotrinn(behandling, aksjonspunktDefinisjoner);
        } else {
            håndterUtenTotrinn(behandling, kontekst, aksjonspunktDefinisjoner);
        }

        return aksjonspunktDefinisjoner.isEmpty()
            ? BehandleStegResultat.utførtUtenAksjonspunkter()
            : BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjoner);
    }

    private void håndterTotrinn(Behandling behandling, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        if (!behandling.isToTrinnsBehandling()) {
            behandling.setToTrinnsBehandling();
            logger.info("To-trinn satt på behandling={}", behandling.getId());
        }
        aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
    }

    private void håndterUtenTotrinn(Behandling behandling, BehandlingskontrollKontekst kontekst, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        behandling.nullstillToTrinnsBehandling();
        logger.info("To-trinn fjernet på behandling={}", behandling.getId());
        settForeslåOgFatterVedtakAksjonspunkterAvbrutt(behandling, kontekst);
        if (skalOppretteForeslåVedtakManuelt(behandling)) {
            aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
        }
    }

    private boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        return finnForeslåVedtakManueltUtleder(behandling).skalOppretteForeslåVedtakManuelt(behandling);
    }

    private ForeslåVedtakManueltUtleder finnForeslåVedtakManueltUtleder(Behandling behandling) {
        FagsakYtelseType ytelseType = behandling.getFagsakYtelseType();
        return FagsakYtelseTypeRef.Lookup.find(foreslåVedtakManueltUtledere, ytelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + ForeslåVedtakManueltUtleder.class.getSimpleName() + " for ytelseType=" + ytelseType));
    }

    private boolean skalUtføreTotrinnsbehandling(Behandling behandling) {
        var totrinn = !behandling.harÅpentAksjonspunktMedType(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL) &&
            behandling.harAksjonspunktMedTotrinnskontroll();

        if (totrinn) {
            var totrinnAks = behandling.getAksjonspunkter().stream()
                .filter(Aksjonspunkt::isToTrinnsBehandling).toList();
            return !totrinnAks.isEmpty();
        }
        return totrinn;
    }

    private void settForeslåOgFatterVedtakAksjonspunkterAvbrutt(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        // TODO: Hører ikke hjemme her. Bør bruke generisk stegresultat eller flyttes. Hva er use-case for disse tilfellene?
        // Er det grunn til å tro at disse finnes når man er i FORVED-steg - de skal utledes i steget?
        List<Aksjonspunkt> skalAvbrytes = new ArrayList<>();
        behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.FORESLÅ_VEDTAK).ifPresent(skalAvbrytes::add);
        behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.FATTER_VEDTAK).ifPresent(skalAvbrytes::add);
        if (!skalAvbrytes.isEmpty()) {
            behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), skalAvbrytes);
        }
    }
}
