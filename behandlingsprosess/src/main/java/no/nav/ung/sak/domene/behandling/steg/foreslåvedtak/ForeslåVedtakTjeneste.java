package no.nav.ung.sak.domene.behandling.steg.foreslåvedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevValidator;
import no.nav.ung.sak.formidling.vedtak.regler.BehandlingVedtaksbrevResultat;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevRegler;
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
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;
    private VedtaksbrevRegler vedtaksbrevRegler;
    private boolean apVedIkkeImplementertBrev;

    protected ForeslåVedtakTjeneste() {
        // CDI proxy
    }

    @Inject
    ForeslåVedtakTjeneste(BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                          SjekkTilbakekrevingAksjonspunktUtleder sjekkMotTilbakekrevingTjeneste,
                          VedtaksbrevValgRepository vedtaksbrevValgRepository,
                          VedtaksbrevRegler vedtaksbrevRegler,
                          @KonfigVerdi(value = "AP_VED_IKKE_IMPLEMENTERT_BREV", defaultVerdi = "false") boolean apVedIkkeImplementertBrev) {
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.sjekkMotTilbakekrevingTjeneste = sjekkMotTilbakekrevingTjeneste;
        this.vedtaksbrevValgRepository = vedtaksbrevValgRepository;
        this.vedtaksbrevRegler = vedtaksbrevRegler;
        this.apVedIkkeImplementertBrev = apVedIkkeImplementertBrev;
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

        if (skalUtføreTotrinnsbehandling(behandling)) {
            håndterTotrinn(behandling, aksjonspunktDefinisjoner);
        } else {
            håndterUtenTotrinn(behandling, kontekst);
        }

        vurderBrev(behandling, aksjonspunktDefinisjoner);

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

    private void håndterUtenTotrinn(Behandling behandling, BehandlingskontrollKontekst kontekst) {
        behandling.nullstillToTrinnsBehandling();
        logger.info("To-trinn fjernet på behandling={}", behandling.getId());
        settForeslåOgFatterVedtakAksjonspunkterAvbrutt(behandling, kontekst);
    }

    private void vurderBrev(Behandling behandling, List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner) {
        var totalResultat = vedtaksbrevRegler.kjør(behandling.getId());

        feilHvisIkkeImplementertBrev(aksjonspunktDefinisjoner, totalResultat);

        var vedtaksbrevValg = vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId());
        håndterBrevSomMåRedigeres(aksjonspunktDefinisjoner, totalResultat, vedtaksbrevValg);

        håndterUgyldigManuellBrev(vedtaksbrevValg);
    }

    private void feilHvisIkkeImplementertBrev(List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner, BehandlingVedtaksbrevResultat totalResultat) {
        if (!totalResultat.harBrev() && totalResultat.ingenBrevResultater().stream()
            .anyMatch(it -> it.ingenBrevÅrsakType() == IngenBrevÅrsakType.IKKE_IMPLEMENTERT)) {
            if (apVedIkkeImplementertBrev) {
                aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
            }
            else {
                throw new IllegalStateException(String.format("Ingen brev implementert - må håndteres manuelt. Forklaring: "
                    + totalResultat.forklaringer()));

            }
        }
    }

    private static void håndterBrevSomMåRedigeres(List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner, BehandlingVedtaksbrevResultat totalResultat, List<VedtaksbrevValgEntitet> vedtaksbrevValg) {
        var brevSomMåRedigeres = totalResultat.brevSomMåRedigeres();
        if (brevSomMåRedigeres.isEmpty()) {
            return;
        }

        var brevSomErHindretEllerRedigert = vedtaksbrevValg.stream()
            .filter(it -> it.isHindret() || it.isRedigert())
            .map(VedtaksbrevValgEntitet::getDokumentMalType)
            .collect(Collectors.toSet());

        if (!brevSomErHindretEllerRedigert.containsAll(brevSomMåRedigeres)) {
            logger.info("Det finnes brev som må skrives manuelt eller hindres: {}", brevSomMåRedigeres);
            aksjonspunktDefinisjoner.add(AksjonspunktDefinisjon.FORESLÅ_VEDTAK_MANUELT);
        }
    }

    private static void håndterUgyldigManuellBrev(List<VedtaksbrevValgEntitet> vedtaksbrevValg) {
        // Feiler hvis redigert brev er ugyldig slik at saksbehandler kan rette på det
        vedtaksbrevValg.stream()
            .filter(it -> !it.isHindret())
            .filter(VedtaksbrevValgEntitet::isRedigert)
            .forEach(it -> ManueltVedtaksbrevValidator.valider(it.getRedigertBrevHtml()));
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
