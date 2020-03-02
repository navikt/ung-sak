package no.nav.foreldrepenger.behandling.steg.vedtak;

import static java.lang.Boolean.TRUE;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.vedtak.VedtakTjeneste;
import no.nav.foreldrepenger.domene.vedtak.repo.LagretVedtakRepository;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveForBehandlingSendtTilbakeTask;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;

@ApplicationScoped
public class FatteVedtakTjeneste {

    public static final String UTVIKLER_FEIL_VEDTAK = "Utvikler-feil: Vedtak kan ikke fattes, behandlingsresultat er ";
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER_REVURDERING = new HashSet<>(
        Arrays.asList(BehandlingResultatType.AVSLÅTT, BehandlingResultatType.INNVILGET,
            BehandlingResultatType.OPPHØR, BehandlingResultatType.INNVILGET_ENDRING,
            BehandlingResultatType.INGEN_ENDRING));
    private static final Set<BehandlingResultatType> VEDTAKSTILSTANDER = new HashSet<>(
        Arrays.asList(BehandlingResultatType.AVSLÅTT, BehandlingResultatType.INNVILGET));

    @SuppressWarnings("unused")
    private LagretVedtakRepository lagretVedtakRepository;
    private VedtakTjeneste vedtakTjeneste;
    private OppgaveTjeneste oppgaveTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private BehandlingVedtakTjeneste behandlingVedtakTjeneste;

    FatteVedtakTjeneste() {
        // for CDI proxy
    }

    @Inject
    public FatteVedtakTjeneste(LagretVedtakRepository vedtakRepository,
                               VedtakTjeneste vedtakTjeneste,
                               OppgaveTjeneste oppgaveTjeneste,
                               TotrinnTjeneste totrinnTjeneste,
                               BehandlingVedtakTjeneste behandlingVedtakTjeneste) {
        this.lagretVedtakRepository = vedtakRepository;
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
                oppgaveTjeneste.avsluttOppgaveOgStartTask(behandling, OppgaveÅrsak.GODKJENNE_VEDTAK, OpprettOppgaveForBehandlingSendtTilbakeTask.TASKTYPE);
                List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = totrinnaksjonspunktvurderinger.stream()
                    .filter(a -> !TRUE.equals(a.isGodkjent()))
                    .map(Totrinnsvurdering::getAksjonspunktDefinisjon).collect(Collectors.toList());

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
                    UTVIKLER_FEIL_VEDTAK // $NON-NLS-1$
                        + (behandlingResultatType.getNavn()));
            }
        } else if (!VEDTAKSTILSTANDER.contains(behandlingResultatType)) {
            throw new IllegalStateException(
                UTVIKLER_FEIL_VEDTAK + behandlingResultatType.getNavn());
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
