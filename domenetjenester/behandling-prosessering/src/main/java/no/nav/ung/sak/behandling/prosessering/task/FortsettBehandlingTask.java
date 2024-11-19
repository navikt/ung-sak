package no.nav.ung.sak.behandling.prosessering.task;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.ung.kodeverk.behandling.BehandlingStegStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.ung.sak.behandlingslager.task.UnderBehandlingProsessTask;

/**
 * Kjører behandlingskontroll automatisk fra der prosessen står.
 */
@ApplicationScoped
@ProsessTask(FortsettBehandlingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class FortsettBehandlingTask extends UnderBehandlingProsessTask {

    public static final String TASKTYPE = "behandlingskontroll.fortsettBehandling";
    public static final String MANUELL_FORTSETTELSE = "manuellFortsettelse";
    public static final String AKSJONSPUNKT_STATUS_TIL_UTFORT = "aksjonspunktStatusTilUtfort";
    public static final String GJENOPPTA_STEG = "gjenopptaSteg";
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    FortsettBehandlingTask() {
        // For CDI proxy
    }

    @Inject
    public FortsettBehandlingTask(BehandlingRepository behandlingRepository,
                                  BehandlingLåsRepository behandlingLåsRepository,
                                  BehandlingskontrollTjeneste behandlingskontrollTjeneste) {
        super(behandlingRepository, behandlingLåsRepository);
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
    }

    @Override
    public void doProsesser(ProsessTaskData data, Behandling behandling) {

        var behandlingId = data.getBehandlingId();
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingId);

        Boolean manuellFortsettelse = Optional.ofNullable(data.getPropertyValue(FortsettBehandlingTask.MANUELL_FORTSETTELSE))
            .map(Boolean::valueOf)
            .orElse(Boolean.FALSE);
        String gjenoppta = data.getPropertyValue(FortsettBehandlingTask.GJENOPPTA_STEG);

        BehandlingStegType stegtype = getBehandlingStegType(gjenoppta);
        if (gjenoppta != null || manuellFortsettelse) {
            if (behandling.isBehandlingPåVent()) { // Autopunkt
                behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
            }
        } else {
            String utført = data.getPropertyValue(FortsettBehandlingTask.AKSJONSPUNKT_STATUS_TIL_UTFORT);
            if (utført != null) {
                var aksjonspunkter = Arrays.stream(utført.split(",\\s*")).map(AksjonspunktDefinisjon::fraKode).collect(Collectors.toList());
                behandlingskontrollTjeneste.settAutopunktTilUtført(behandling, kontekst, aksjonspunkter);
            }
        }
        // Ingen åpne autopunkt her, takk
        validerBehandlingIkkeErSattPåVent(behandling);

        // Sjekke om kan prosesserere, samt feilhåndtering vs savepoint: Ved retry av feilet task som har passert gjenopptak må man fortsette.
        Optional<BehandlingStegTilstand> tilstand = behandling.getBehandlingStegTilstand();
        if (gjenoppta != null && tilstand.isPresent() && tilstand.get().getBehandlingSteg().equals(stegtype)
            && BehandlingStegStatus.VENTER.equals(tilstand.get().getBehandlingStegStatus())) {
            behandlingskontrollTjeneste.prosesserBehandlingGjenopptaHvisStegVenter(kontekst, stegtype);
        } else if (!behandling.erAvsluttet()) {
            behandlingskontrollTjeneste.prosesserBehandling(kontekst);
        }

    }

    private BehandlingStegType getBehandlingStegType(String gjenopptaSteg) {
        if (gjenopptaSteg == null) {
            return null;
        }
        BehandlingStegType stegtype = BehandlingStegType.fraKode(gjenopptaSteg);
        if (stegtype == null) {
            throw new IllegalStateException("Utviklerfeil: ukjent steg " + gjenopptaSteg);
        }
        return stegtype;
    }

    private void validerBehandlingIkkeErSattPåVent(Behandling behandling) {
        if (behandling.isBehandlingPåVent()) {
            throw new IllegalStateException("Utviklerfeil: Ikke tillatt å fortsette behandling på vent");
        }
    }
}
