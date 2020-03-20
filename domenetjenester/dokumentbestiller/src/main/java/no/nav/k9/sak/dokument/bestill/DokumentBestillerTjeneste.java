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
    private VedtakVarselRepository vedtakVarselRepository;
    
    DokumentBestillerTjeneste() {
        // CDI
    }

    @Inject
    public DokumentBestillerTjeneste(@KonfigVerdi(value = "behandling.default.ventefrist.periode", defaultVerdi = "P4W") Period defaultVenteFrist,
                                     OppgaveTjeneste oppgaveTjeneste,
                                     VedtakVarselRepository vedtakVarselRepository,
                                     OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository,
                                     BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                     DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste) {
        this.defaultVenteFrist = defaultVenteFrist;
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.vedtakVarselRepository = vedtakVarselRepository;
        this.oppgaveBehandlingKoblingRepository = oppgaveBehandlingKoblingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
    }

    public void håndterVarselRevurdering(Behandling behandling, VarselRevurderingAksjonspunkt adapter) {
        new VarselRevurderingHåndterer(defaultVenteFrist, oppgaveBehandlingKoblingRepository, vedtakVarselRepository, oppgaveTjeneste, behandlingskontrollTjeneste, dokumentBestillerApplikasjonTjeneste)
            .oppdater(behandling, adapter);
    }
}
