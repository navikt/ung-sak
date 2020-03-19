package no.nav.k9.sak.domene.vedtak.intern;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.dokument.bestill.DokumentBehandlingTjeneste;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;

@ApplicationScoped
public class SendVedtaksbrev {

    private static final Logger log = LoggerFactory.getLogger(SendVedtaksbrev.class);

    private BehandlingRepository behandlingRepository;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    private BehandlingVedtakRepository behandlingVedtakRepository;

    private VedtakVarselRepository behandlingsresultatRepository;

    SendVedtaksbrev() {
        // for CDI proxy
    }

    @Inject
    public SendVedtaksbrev(BehandlingRepository behandlingRepository,
                           BehandlingVedtakRepository behandlingVedtakRepository,
                           VedtakVarselRepository behandlingsresultatRepository,
                           DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste,
                           DokumentBehandlingTjeneste dokumentBehandlingTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.behandlingsresultatRepository = behandlingsresultatRepository;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
    }

    void sendVedtaksbrev(String behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        sendVedtaksbrev(BehandlingReferanse.fra(behandling));
    }

    void sendVedtaksbrev(BehandlingReferanse ref) {

        Long behandlingId = ref.getBehandlingId();
        Optional<BehandlingVedtak> behandlingVedtakOpt = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandlingId);
        if (behandlingVedtakOpt.isEmpty()) {
            log.info("Det foreligger ikke vedtak i behandling: {}, kan ikke sende vedtaksbrev", ref); //$NON-NLS-1$
            return;
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (Fagsystem.INFOTRYGD.equals(behandling.getMigrertKilde())) {
            var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
            boolean fritekstVedtaksbrev = Vedtaksbrev.FRITEKST.equals(behandlingsresultat.getVedtaksbrev());
            if (!fritekstVedtaksbrev) {
                log.info("Sender ikke vedtaksbrev for sak som er migrert fra Infotrygd. Gjelder behandlingId {}", ref);
                return;
            }
        }

        var behandlingVedtak = behandlingVedtakOpt.get();
        if (behandlingVedtak.isBeslutningsvedtak()) {
            log.info("Sender informasjonsbrev om uendret utfall i behandling: {}", ref); //$NON-NLS-1$
        } else {
            log.info("Sender vedtaksbrev({}) for foreldrepenger i behandling: {}", behandlingVedtak.getVedtakResultatType(), ref); // $NON-NLS-1
        }
        dokumentBestillerApplikasjonTjeneste.produserVedtaksbrev(ref, behandlingVedtak);
    }

}
