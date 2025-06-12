package no.nav.ung.sak.formidling;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.felles.integrasjon.saf.Saf;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.ung.sak.formidling.bestilling.BrevbestillingResultat;

@Dependent
public class BrevHistorikkinnslagTjeneste {
    private final HistorikkinnslagRepository historikkRepository;
    private Saf safTjeneste;

    @Inject
    public BrevHistorikkinnslagTjeneste(HistorikkinnslagRepository historikkRepository, Saf safTjeneste) {
        this.historikkRepository = historikkRepository;
        this.safTjeneste = safTjeneste;
    }

    public void opprett(HistorikkAktør historikkAktør,
                        Behandling behandling,
                        GenerertBrev generertBrev,
                        BrevbestillingResultat brevbestillingResultat) {

        var builder = new Historikkinnslag.Builder();
        builder.medBehandlingId(behandling.getId());
        builder.medAktør(historikkAktør);
        builder.medTittel("Brev bestilt");
        builder.addLinje(generertBrev.templateType().getBeskrivelse());
        historikkRepository.lagre(builder.build());
    }

}
