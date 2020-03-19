package no.nav.k9.sak.dokument.bestill;

import static no.nav.k9.sak.dokument.bestill.vedtak.VedtaksbrevUtleder.velgDokumentMalForVedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.dokument.bestill.kafka.DokumentKafkaBestiller;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;

@ApplicationScoped
public class DokumentBestillerApplikasjonTjeneste {

    private BehandlingRepository behandlingRepository;

    private DokumentKafkaBestiller dokumentKafkaBestiller;
    private VedtakVarselRepository behandlingsresultatRepository;


    public DokumentBestillerApplikasjonTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentBestillerApplikasjonTjeneste(BehandlingRepository behandlingRepository,
                                                VedtakVarselRepository behandlingsresultatRepository,
                                                DokumentKafkaBestiller dokumentKafkaBestiller) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.dokumentKafkaBestiller = dokumentKafkaBestiller;
    }

    public void produserVedtaksbrev(BehandlingReferanse ref, BehandlingVedtak behandlingVedtak) {
        Long behandlingId = behandlingVedtak.getBehandlingId();
        var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        if (Vedtaksbrev.INGEN.equals(behandlingsresultat.getVedtaksbrev())) {
            return;
        }
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        DokumentMalType dokumentMal = velgDokumentMalForVedtak(ref, behandlingsresultat, behandlingVedtak);
        dokumentKafkaBestiller.bestillBrev(behandling, dokumentMal, null, null, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    public void bestillDokument(BestillBrevDto bestillBrevDto, HistorikkAktør aktør) {
        dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);
    }
}
