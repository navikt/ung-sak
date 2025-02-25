package no.nav.ung.sak.domene.behandling.steg.vedtak;

import static java.lang.Boolean.TRUE;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.vedtak.VedtakTjeneste;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;

@ApplicationScoped
public class FatteVedtakTjeneste {

    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_REVURDERING = new HashSet<>(
        Arrays.asList(BehandlingResultatType.AVSLÅTT, BehandlingResultatType.INNVILGET,
            BehandlingResultatType.OPPHØR, BehandlingResultatType.INNVILGET_ENDRING,
            BehandlingResultatType.INGEN_ENDRING, BehandlingResultatType.DELVIS_INNVILGET));
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER = new HashSet<>(
        Arrays.asList(BehandlingResultatType.AVSLÅTT, BehandlingResultatType.INNVILGET, BehandlingResultatType.DELVIS_INNVILGET));

    private VedtakTjeneste vedtakTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingVedtakTjeneste behandlingVedtakTjeneste;

    FatteVedtakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FatteVedtakTjeneste(VedtakTjeneste vedtakTjeneste,
                               OppgaveTjeneste oppgaveTjeneste,
                               TotrinnTjeneste totrinnTjeneste,
                               BehandlingVedtakTjeneste behandlingVedtakTjeneste) {
        this.vedtakTjeneste = vedtakTjeneste;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
        this.behandlingVedtakTjeneste = behandlingVedtakTjeneste;
    }

    public BehandleStegResultat fattVedtak(BehandlingskontrollKontekst kontekst, Behandling behandling) {
        verifiserBehandlingsresultat(behandling);
        if (behandling.isToTrinnsBehandling()) {
            Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
            if (sendesTilbakeTilSaksbehandler(totrinnaksjonspunktvurderinger)) {
                // FIXME K9-LOS: Trengs hendelse her?
                List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = totrinnaksjonspunktvurderinger.stream()
                    .filter(a -> !TRUE.equals(a.isGodkjent()))
                    .map(Totrinnsvurdering::getAksjonspunktDefinisjon)
                    .collect(Collectors.toList());

                return BehandleStegResultat.tilbakeførtMedAksjonspunkter(aksjonspunktDefinisjoner);
            } else {
                oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling);
            }
        } else {
            vedtakTjeneste.lagHistorikkinnslagFattVedtak(behandling);
        }

        behandlingVedtakTjeneste.opprettBehandlingVedtak(kontekst, behandling);

        opprettLagretVedtak(behandling);

        // Ingen nye aksjonspunkt herfra
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean sendesTilbakeTilSaksbehandler(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream()
            .anyMatch(a -> !TRUE.equals(a.isGodkjent()));
    }

    private void verifiserBehandlingsresultat(Behandling behandling) { // NOSONAR dette er bare enkel verifisering og har ikke høy complexity
        BehandlingResultatType behandlingResultatType = behandling.getBehandlingResultatType();
        if (behandling.erRevurdering()) {
            if (!VEDTAKSTILSTANDER_REVURDERING.contains(behandlingResultatType)) {
                throw new IllegalStateException(
                    "Utvikler-feil: Vedtak kan ikke fattes for [" + behandling.getType() + "], behandlingsresultat er " // $NON-NLS-1$
                        + (behandlingResultatType.getNavn()));
            }
        } else if (!VEDTAKSTILSTANDER.contains(behandlingResultatType)) {
            throw new IllegalStateException(
                "Utvikler-feil: Vedtak kan ikke fattes for behandling [" + behandling.getType() + "], behandlingsresultat er " + behandlingResultatType.getNavn());
        }
    }

    private void opprettLagretVedtak(Behandling behandling) {
        if (!erKlarForVedtak(behandling)) {
            throw new IllegalStateException("Behandlig er ikke klar for vedtak : " + behandling.getId() + ", status=" + behandling.getStatus().getKode());
        }
        // FIXME K9 - lagre vedtak
    }

    private boolean erKlarForVedtak(Behandling behandling) {
        return BehandlingStatus.FATTER_VEDTAK.equals(behandling.getStatus());
    }

}
