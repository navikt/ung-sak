package no.nav.ung.sak.klage;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.historikk.HistorikkAktør;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagRepository;

/** Lag historikk innslag ved klage. */
@Dependent
public class KlageHistorikkTjeneste {
    private HistorikkinnslagRepository historikkRepository;

    KlageHistorikkTjeneste() {
        // For CDI
    }

    @Inject
    public KlageHistorikkTjeneste(HistorikkinnslagRepository historikkRepository) {
        this.historikkRepository = historikkRepository;
    }

    public void opprettHistorikkinnslag(Behandling klageBehandling) {
        Historikkinnslag historikkinnslag = new Historikkinnslag.Builder()
            .medAktør(HistorikkAktør.SØKER)
//                .med(HistorikkinnslagType.BEH_STARTET)
                    .medBehandlingId(klageBehandling.getId())
                        .medFagsakId(klageBehandling.getFagsakId())
            .build();
//
//        HistorikkInnslagTekstBuilder builder = new HistorikkInnslagTekstBuilder()
//            .medHendelse(BehandlingType.KLAGE.equals(klageBehandling.getType()) ? HistorikkinnslagType.KLAGEBEH_STARTET : HistorikkinnslagType.BEH_STARTET);
//        builder.build(historikkinnslag);

        historikkRepository.lagre(historikkinnslag);
    }
}
