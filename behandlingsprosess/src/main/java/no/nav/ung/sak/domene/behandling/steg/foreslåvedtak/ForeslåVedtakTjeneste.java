package no.nav.ung.sak.domene.behandling.steg.foreslåvedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevValidator;
import no.nav.ung.sak.domene.vedtak.impl.KlageVedtakTjeneste;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevTjeneste;
import no.nav.ung.sak.økonomi.tilbakekreving.samkjøring.SjekkTilbakekrevingAksjonspunktUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
class ForeslåVedtakTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForeslåVedtakTjeneste.class);

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private SjekkTilbakekrevingAksjonspunktUtleder sjekkMotTilbakekrevingTjeneste;
    private VedtaksbrevTjeneste vedtaksbrevTjeneste;
    private KlageVedtakTjeneste klageVedtakTjeneste;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;

    protected ForeslåVedtakTjeneste() {
        // CDI proxy
    }

    @Inject
    ForeslåVedtakTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                          SjekkTilbakekrevingAksjonspunktUtleder sjekkMotTilbakekrevingTjeneste,
                          VedtaksbrevTjeneste vedtaksbrevTjeneste,
                          KlageVedtakTjeneste klageVedtakTjeneste,
                          VedtaksbrevValgRepository vedtaksbrevValgRepository) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.sjekkMotTilbakekrevingTjeneste = sjekkMotTilbakekrevingTjeneste;
        this.vedtaksbrevTjeneste = vedtaksbrevTjeneste;
        this.klageVedtakTjeneste = klageVedtakTjeneste;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
    }

    public BehandleStegResultat foreslåVedtak(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = new ArrayList<>();
        // TODO: Fiks integrering mot k9-tilbake
//        aksjonspunktDefinisjoner.addAll(sjekkMotTilbakekrevingTjeneste.sjekkMotÅpenIkkeoverlappendeTilbakekreving(behandling));

        if (BehandlingType.KLAGE.equals(behandling.getType())) {
            if (klageVedtakTjeneste.erKlageResultatHjemsendt(behandling)) {
                behandling.nullstillToTrinnsBehandling();
                return BehandleStegResultat.utførtUtenAksjonspunkter();
            }
        }

        Optional<Aksjonspunkt> vedtakUtenTotrinnskontroll = behandling.getÅpentAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VEDTAK_UTEN_TOTRINNSKONTROLL);
        if (vedtakUtenTotrinnskontroll.isPresent()) {
            behandling.nullstillToTrinnsBehandling();
            return BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjoner);
        }

        if (skalUtføreTotrinnsbehandling(behandling)) {
            håndterTotrinn(behandling, aksjonspunktDefinisjoner);
        } else {
            håndterUtenTotrinn(behandling, kontekst, aksjonspunktDefinisjoner);
        }

        // Feiler hvis redigert brev er ugyldig slik at saksbehandler kan rette på det
        validerEvtRedigertBrevHtml(behandling);

        return aksjonspunktDefinisjoner.isEmpty()
            ? BehandleStegResultat.utførtUtenAksjonspunkter()
            : BehandleStegResultat.utførtMedAksjonspunkter(aksjonspunktDefinisjoner);
    }

    private void validerEvtRedigertBrevHtml(Behandling behandling) {
        var vedtaksbrevValg = vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId());
        vedtaksbrevValg.stream()
            .filter(it -> !it.isHindret())
            .filter(VedtaksbrevValgEntitet::isRedigert)
            .forEach(it -> ManueltVedtaksbrevValidator.valider(it.getRedigertBrevHtml()));
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
        vurderBrevRedigering(behandling, aksjonspunktDefinisjoner);
    }

    private void vurderBrevRedigering(Behandling behandling, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        var brevSomMåRedigeres = vedtaksbrevTjeneste.måSkriveBrev(behandling.getId());
        if (brevSomMåRedigeres.isEmpty()) {
            return;
        }

        var brevSomErHindretEllerRedigert = vedtaksbrevValgRepository
            .finnVedtakbrevValg(behandling.getId()).stream()
            .filter(it -> it.isHindret() || it.isRedigert())
            .map(VedtaksbrevValgEntitet::getDokumentMalType)
            .collect(Collectors.toSet());

        if (!brevSomErHindretEllerRedigert.containsAll(brevSomMåRedigeres)) {
            logger.info("Det finnes brev som må skrives manuelt eller hindres: {}", brevSomMåRedigeres);
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
