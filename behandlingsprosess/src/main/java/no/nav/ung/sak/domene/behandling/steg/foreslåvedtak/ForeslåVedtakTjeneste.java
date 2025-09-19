package no.nav.ung.sak.domene.behandling.steg.foreslåvedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevTjeneste;
import no.nav.ung.sak.økonomi.tilbakekreving.samkjøring.SjekkTilbakekrevingAksjonspunktUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
class ForeslåVedtakTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForeslåVedtakTjeneste.class);

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private SjekkTilbakekrevingAksjonspunktUtleder sjekkMotTilbakekrevingTjeneste;
    private VedtaksbrevTjeneste vedtaksbrevTjeneste;

    protected ForeslåVedtakTjeneste() {
        // CDI proxy
    }

    @Inject
    ForeslåVedtakTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                          SjekkTilbakekrevingAksjonspunktUtleder sjekkMotTilbakekrevingTjeneste,
                          VedtaksbrevTjeneste vedtaksbrevTjeneste) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.sjekkMotTilbakekrevingTjeneste = sjekkMotTilbakekrevingTjeneste;
        this.vedtaksbrevTjeneste = vedtaksbrevTjeneste;
    }

    public BehandleStegResultat foreslåVedtak(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new ArrayList<>();
        // TODO: Fiks integrering mot k9-tilbake
//        aksjonspunktDefinisjoner.addAll(sjekkMotTilbakekrevingTjeneste.sjekkMotÅpenIkkeoverlappendeTilbakekreving(behandling));

        Optional<Aksjonspunkt> vedtakUtenTotrinnskontroll = behandling
            .getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL);
        if (vedtakUtenTotrinnskontroll.isPresent()) {
            behandling.nullstillToTrinnsBehandling();
            return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjoner);
        }

        //TODO resett brevvalg! Slett gamle tekster og kopier over hvis fortsatt relevante.

        if (skalUtføreTotrinnsbehandling(behandling)) {
            håndterTotrinn(behandling, aksjonspunktDefinisjoner);
        } else {
            håndterUtenTotrinn(behandling, kontekst, aksjonspunktDefinisjoner);
        }

        // Logging av automatisering av endringsmelding
        if (aksjonspunktDefinisjoner.isEmpty()
            && behandling.getFagsakYtelseType().equals(FagsakYtelseType.PLEIEPENGER_SYKT_BARN)
            && behandling.erRevurdering()
            && behandling.getBehandlingÅrsakerTyper().stream().allMatch(årsak -> årsak.equals(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE))) {
            logger.info("Foreslår vedtak uten aksjonspunkter");
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
        if (vedtaksbrevTjeneste.måSkriveBrev(behandling.getId())) {
            aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
        }
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
        final var fatterVedtakAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.FATTER_VEDTAK).filter(it -> !it.erAvbrutt());
        if (fatterVedtakAksjonspunkt.isPresent()) {
            throw new IllegalStateException("Hadde fatter vedtak aksjonspunkt som ikke allerede var avbrutt i foreslå vedtak uten totrinnsvurdering på behandling: "  + fatterVedtakAksjonspunkt.get());
        }
        if (!skalAvbrytes.isEmpty()) {
            behandlingskontrollTjeneste.lagreAksjonspunkterAvbrutt(kontekst, behandling.getAktivtBehandlingSteg(), skalAvbrytes);
        }
    }
}
