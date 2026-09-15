package no.nav.ung.sak.etterlysning.historikkinnslag;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;

@Dependent
public class OppgaveHistorikkinnslagTjeneste {

    private HistorikkinnslagRepository historikkinnslagRepository;

    @Inject
    public OppgaveHistorikkinnslagTjeneste(HistorikkinnslagRepository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }


}
