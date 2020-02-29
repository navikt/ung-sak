package no.nav.foreldrepenger.dokumentbestiller;

import static no.nav.foreldrepenger.dokumentbestiller.vedtak.VedtaksbrevUtleder.velgDokumentMalForVedtak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.dokumentbestiller.kafka.DokumentKafkaBestiller;
import no.nav.foreldrepenger.dokumentbestiller.klient.FormidlingRestKlient;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.sak.kontrakt.dokument.BestillBrevDto;

@ApplicationScoped
public class DokumentBestillerApplikasjonTjeneste {

    private BehandlingRepository behandlingRepository;
    private FormidlingRestKlient formidlingRestKlient;

    private BrevHistorikkinnslag brevHistorikkinnslag;
    private DokumentKafkaBestiller dokumentKafkaBestiller;
    private BehandlingsresultatRepository behandlingsresultatRepository;


    public DokumentBestillerApplikasjonTjeneste() {
        // for cdi proxy
    }

    @Inject
    public DokumentBestillerApplikasjonTjeneste(BehandlingRepository behandlingRepository,
                                                BehandlingsresultatRepository behandlingsresultatRepository,
                                                BrevHistorikkinnslag brevHistorikkinnslag,
                                                FormidlingRestKlient formidlingRestKlient,
                                                DokumentKafkaBestiller dokumentKafkaBestiller) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.brevHistorikkinnslag = brevHistorikkinnslag;
        this.dokumentKafkaBestiller = dokumentKafkaBestiller;
        this.formidlingRestKlient = formidlingRestKlient;
    }

    public void produserVedtaksbrev(BehandlingVedtak behandlingVedtak) {
        var behandlingsresultat = behandlingsresultatRepository.hent(behandlingVedtak.getBehandlingId());
        if (Vedtaksbrev.INGEN.equals(behandlingsresultat.getVedtaksbrev())) {
            return;
        }

        DokumentMalType dokumentMal = velgDokumentMalForVedtak(behandlingsresultat, behandlingVedtak);
        dokumentKafkaBestiller.bestillBrev(behandlingsresultat.getBehandling(), dokumentMal, null, null, HistorikkAktør.VEDTAKSLØSNINGEN);
    }

    public void bestillDokument(BestillBrevDto bestillBrevDto, HistorikkAktør aktør) {
        dokumentKafkaBestiller.bestillBrevFraKafka(bestillBrevDto, aktør);
    }
}
