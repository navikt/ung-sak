package no.nav.ung.sak.domene.behandling.steg.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.domene.vedtak.VedtakTjeneste;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;

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

            final var fatterVedtakAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.FATTER_VEDTAK);

            // Dersom vi ikke har fatter vedtak aksjonspunkt eller allerede har opprettet aksjonspunkt og behandlingen er flagget som totrinnsbehandling returnerer vi med aksjonspunkt og går videre til steg-ut
            if (fatterVedtakAksjonspunkt.isEmpty() || fatterVedtakAksjonspunkt.filter(Aksjonspunkt::erÅpentAksjonspunkt).isPresent()) {
                return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.FATTER_VEDTAK));
            }

            Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger = totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling);
            // Sjekker om vi har minst en ikke godkjent vurdering og om behandlingen skal flyttes tilbake
            if (sendesTilbakeTilSaksbehandler(totrinnaksjonspunktvurderinger)) {
                List<AksjonspunktDefinisjon> aksjonspunktDefinisjoner = finnIkkeGodkjenteVurderinger(totrinnaksjonspunktvurderinger);
                // Flytter behandling tilbake til første ikke-godkjente aksjonspunkt
                return BehandleStegResultat.tilbakeførtMedAksjonspunkter(aksjonspunktDefinisjoner);
            } else if (harUtførtAksjonspunktOgGodkjentAlleVurderinger(fatterVedtakAksjonspunkt.get(), totrinnaksjonspunktvurderinger)) {
                // Dersom alle vurderinger er godkjent og aksjonspunktet er utført går vi videre
                // Avslutter eventuelt åpne oppgaver i gosys
                // TODO: Vurder om dette trenger å ligge her, kan det ligge i iverksetting?
                oppgaveTjeneste.opprettTaskAvsluttOppgave(behandling);
            } else {
                throw new IllegalStateException("Kunne ikke fatte vedtak. Hadde aksjonspunkt med status " + fatterVedtakAksjonspunkt.get().getStatus() + " og totrinnsvurderinger: " + totrinnaksjonspunktvurderinger);
            }
        } else {
            vedtakTjeneste.lagHistorikkinnslagFattVedtak(behandling);
        }


        // Her har vi enten ikke totrinnskontroll eller gjennomført og godkjent totrinnskontroll

        behandlingVedtakTjeneste.opprettBehandlingVedtak(kontekst, behandling);

        opprettLagretVedtak(behandling);

        // Ingen nye aksjonspunkt herfra
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private static boolean harUtførtAksjonspunktOgGodkjentAlleVurderinger(Aksjonspunkt fatterVedtakAksjonspunkt, Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger) {
        return fatterVedtakAksjonspunkt.getStatus() == AksjonspunktStatus.UTFØRT && !totrinnaksjonspunktvurderinger.isEmpty() && totrinnaksjonspunktvurderinger.stream().allMatch(Totrinnsvurdering::isGodkjent);
    }

    private static List<AksjonspunktDefinisjon> finnIkkeGodkjenteVurderinger(Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger) {
        return totrinnaksjonspunktvurderinger.stream()
            .filter(a -> !a.isGodkjent())
            .map(Totrinnsvurdering::getAksjonspunktDefinisjon)
            .collect(Collectors.toList());
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
