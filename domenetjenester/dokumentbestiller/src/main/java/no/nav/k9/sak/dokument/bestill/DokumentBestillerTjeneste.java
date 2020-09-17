package no.nav.k9.sak.dokument.bestill;

import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveBehandlingKoblingRepository;
import no.nav.k9.sak.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class DokumentBestillerTjeneste {

    private Period defaultVenteFrist;
    private OppgaveTjeneste oppgaveTjeneste;
    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;

    DokumentBestillerTjeneste() {
        // CDI
    }

    @Inject
    public DokumentBestillerTjeneste(@KonfigVerdi(value = "behandling.default.ventefrist.periode", defaultVerdi = "P4W") Period defaultVenteFrist,
                                     OppgaveTjeneste oppgaveTjeneste,
                                     OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                     BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                     DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste) {
        this.defaultVenteFrist = defaultVenteFrist;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
    }

    public void håndterVarselRevurdering(Behandling behandling, VarselRevurderingAksjonspunkt adapter) {
        new VarselRevurderingHåndterer(defaultVenteFrist, oppgaveBehandlingKoblingRepository, oppgaveTjeneste, behandlingskontrollTjeneste, dokumentBestillerApplikasjonTjeneste)
            .oppdater(behandling, adapter);
    }
}
