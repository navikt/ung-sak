package no.nav.ung.sak.domene.behandling.steg.iverksettevedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.domene.vedtak.VedtakTjeneste;
import no.nav.ung.sak.domene.vedtak.intern.AvsluttBehandlingTask;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Boolean.TRUE;
import static no.nav.ung.kodeverk.behandling.BehandlingStegType.AVSLUTT_BEHANDLING;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.PERIODER;

@BehandlingStegRef(value = AVSLUTT_BEHANDLING)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class AvsluttBehandlingSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(AvsluttBehandlingSteg.class);
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private BehandlingRepository behandlingRepository;
    private OppgaveTjeneste oppgaveTjeneste;
    private TotrinnTjeneste totrinnTjeneste;


    AvsluttBehandlingSteg() {
        // for CDI proxy
    }

    @Inject
    public AvsluttBehandlingSteg(ProsessTaskTjeneste prosessTaskTjeneste, ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder, BehandlingRepository behandlingRepository, OppgaveTjeneste oppgaveTjeneste, TotrinnTjeneste totrinnTjeneste) {
        this.prosessTaskTjeneste = prosessTaskTjeneste;
        this.prosessTriggerPeriodeUtleder = prosessTriggerPeriodeUtleder;
        this.behandlingRepository = behandlingRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    @Override
    public final BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();


        final var behandling = behandlingRepository.hentBehandling(behandlingId);

        if (behandling.harAksjonspunktMedTotrinnskontroll()) {

            behandling.setToTrinnsBehandling();

            final var fatterVedtakAksjonspunkt = behandling.getAksjonspunktMedDefinisjonOptional(AksjonspunktDefinisjon.VURDER_SAKSBEHANDLERS_VURDERINGER);

            // Dersom vi ikke har fatter vedtak aksjonspunkt eller allerede har opprettet aksjonspunkt og behandlingen er flagget som totrinnsbehandling returnerer vi med aksjonspunkt og går videre til steg-ut
            if (fatterVedtakAksjonspunkt.isEmpty() || fatterVedtakAksjonspunkt.filter(Aksjonspunkt::erÅpentAksjonspunkt).isPresent()) {
                return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.VURDER_SAKSBEHANDLERS_VURDERINGER));
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
            prosessTaskTjeneste.lagre(lagRevurderingTask(kontekst.getFagsakId(), prosessTriggerPeriodeUtleder.utledTidslinje(kontekst.getBehandlingId()).getLocalDateIntervals()));
        }

        // TODO: Lag historikk
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private ProsessTaskData lagRevurderingTask(Long fagsakId, Set<LocalDateInterval> perioder) {
        ProsessTaskData tilVurderingTask = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        tilVurderingTask.setFagsakId(fagsakId);
        final var perioderString = perioder.stream().map(it -> it.getFomDato() + "/" + it.getTomDato())
            .collect(Collectors.joining("|"));
        tilVurderingTask.setProperty(PERIODER, perioderString);
        tilVurderingTask.setProperty(BEHANDLING_ÅRSAK, BehandlingÅrsakType.RE_ENDRET_KONTROLLPERIODER.getKode());
        return tilVurderingTask;
    }

    private static List<AksjonspunktDefinisjon> finnIkkeGodkjenteVurderinger(Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger) {
        return totrinnaksjonspunktvurderinger.stream()
            .filter(a -> !a.isGodkjent())
            .map(Totrinnsvurdering::getAksjonspunktDefinisjon)
            .collect(Collectors.toList());
    }


    private static boolean harUtførtAksjonspunktOgGodkjentAlleVurderinger(Aksjonspunkt fatterVedtakAksjonspunkt, Collection<Totrinnsvurdering> totrinnaksjonspunktvurderinger) {
        return fatterVedtakAksjonspunkt.getStatus() == AksjonspunktStatus.UTFØRT && !totrinnaksjonspunktvurderinger.isEmpty() && totrinnaksjonspunktvurderinger.stream().allMatch(Totrinnsvurdering::isGodkjent);
    }

    private boolean sendesTilbakeTilSaksbehandler(Collection<Totrinnsvurdering> medTotrinnskontroll) {
        return medTotrinnskontroll.stream()
            .anyMatch(a -> !TRUE.equals(a.isGodkjent()));
    }

}
