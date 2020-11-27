package no.nav.k9.sak.behandling.prosessering.task;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegStatus;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingStegTilstand;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.k9.sak.behandlingslager.task.UnderBehandlingProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

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
                var aksjonspunkter = Arrays.asList(utført.split(",\\s*")).stream().map(v -> AksjonspunktDefinisjon.fraKode(v)).collect(Collectors.toList());
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
