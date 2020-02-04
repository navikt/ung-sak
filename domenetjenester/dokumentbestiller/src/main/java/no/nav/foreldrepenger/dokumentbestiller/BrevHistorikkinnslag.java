package no.nav.foreldrepenger.dokumentbestiller;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;

@ApplicationScoped
public class BrevHistorikkinnslag {
    private HistorikkRepository historikkRepository;

    public BrevHistorikkinnslag() {
        // for cdi proxy
    }

    @Inject
    public BrevHistorikkinnslag(HistorikkRepository historikkRepository) {
        this.historikkRepository = historikkRepository;
    }

    public void opprettHistorikkinnslagForBestiltBrevFraKafka(HistorikkAktør historikkAktør,
                                                              Behandling behandling,
                                                              DokumentMalType dokumentMalType) {
        Historikkinnslag historikkinnslag = new Historikkinnslag();
        historikkinnslag.setBehandling(behandling);
        historikkinnslag.setAktør(historikkAktør);
        historikkinnslag.setType(HistorikkinnslagType.BREV_BESTILT);

        new HistorikkInnslagTekstBuilder().medHendelse(HistorikkinnslagType.BREV_BESTILT)
            .medBegrunnelse(dokumentMalType.getNavn())
            .build(historikkinnslag);
        historikkRepository.lagre(historikkinnslag);
    }

    void opprettHistorikkinnslagForManueltBestiltBrev(HistorikkAktør historikkAktør,
                                                      Behandling behandling,
                                                      String dokumentMal) {
        DokumentMalType dokumentMalType = DokumentMalType.fraKode(dokumentMal);
        opprettHistorikkinnslagForBestiltBrevFraKafka(historikkAktør, behandling, dokumentMalType);
    }
}
