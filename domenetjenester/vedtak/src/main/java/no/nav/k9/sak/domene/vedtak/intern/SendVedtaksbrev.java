package no.nav.k9.sak.domene.vedtak.intern;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.kodeverk.Fagsystem;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;

@FagsakYtelseTypeRef
@ApplicationScoped
public class SendVedtaksbrev {

    private static final Logger log = LoggerFactory.getLogger(SendVedtaksbrev.class);

    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste;

    private BehandlingVedtakRepository behandlingVedtakRepository;

    private VedtakVarselRepository vedtakvarselRepository;

    protected SendVedtaksbrev() {
        // for CDI proxy
    }

    @Inject
    public SendVedtaksbrev(BehandlingRepository behandlingRepository,
                           FagsakRepository fagsakRepository,
                           BehandlingVedtakRepository behandlingVedtakRepository,
                           VedtakVarselRepository vedtakvarselRepository,
                           DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.fagsakRepository = fagsakRepository;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.vedtakvarselRepository = vedtakvarselRepository;
        this.dokumentBestillerApplikasjonTjeneste = dokumentBestillerApplikasjonTjeneste;
    }

    public void sendVedtaksbrev(String behandlingId) {
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);
        if (skalSendeVedtaksbrev(ref)) {
            behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(ref.getBehandlingId())
                .ifPresent(behandlingVedtak -> {
                    dokumentBestillerApplikasjonTjeneste.produserVedtaksbrev(ref, behandlingVedtak);

                    if (behandlingVedtak.isBeslutningsvedtak()) {
                        log.info("Sender informasjonsbrev om uendret utfall i behandling: {}", behandlingId); //$NON-NLS-1$
                    } else {
                        log.info("Sender vedtaksbrev({}) for {} i behandling: {}", behandlingVedtak.getVedtakResultatType(), ref.getFagsakYtelseType(), behandlingId); // $NON-NLS-1
                    }
                });
        }
    }

    protected boolean skalSendeVedtaksbrev(BehandlingReferanse ref) {
        Long behandlingId = ref.getBehandlingId();
        Optional<BehandlingVedtak> behandlingVedtakOpt = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandlingId);
        if (behandlingVedtakOpt.isEmpty()) {
            log.info("Det foreligger ikke vedtak i behandling: {}, kan ikke sende vedtaksbrev", behandlingId); //$NON-NLS-1$
            return false;
        }

        if (erKunRefusjonTilArbeidsgiver(ref)) {
            log.info("Sender ikke vedtaksbrev for omsorgspenger - refusjon til arbeidsgiver. Gjelder behandlingId {}", behandlingId); //$NON-NLS-1$
            return false;
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        if (Fagsystem.INFOTRYGD.equals(behandling.getMigrertKilde())) {
            var behandlingsresultat = vedtakvarselRepository.hent(behandlingId);
            boolean fritekstVedtaksbrev = Vedtaksbrev.FRITEKST.equals(behandlingsresultat.getVedtaksbrev());
            if (!fritekstVedtaksbrev) {
                log.info("Sender ikke vedtaksbrev for sak som er migrert fra Infotrygd. Gjelder behandlingId {}", behandlingId);
                return false;
            }
        }

        if (skalTilInfoTrygd(ref)) {
            log.info("Sender ikke vedtaksbrev for behandlinger som er henlag og overført til infortrygd. BehandlingId {}", behandlingId); //$NON-NLS-1$
            return false;
        }

        if (erBehandlingEtterKlage(behandling)) {
            log.info("Sender ikke vedtaksbrev for vedtak fra omgjøring fra klageinstansen på behandling {}, gjelder medhold fra klageinstans", behandlingId); //$NON-NLS-1$
            return false;
        }
        return true;
    }

    protected boolean erKunRefusjonTilArbeidsgiver(BehandlingReferanse ref) {
        return ref.getFagsakYtelseType() == FagsakYtelseType.OMSORGSPENGER;
    }

    protected boolean erBehandlingEtterKlage(Behandling behandling) {
        return BehandlingÅrsakType.årsakerEtterKlageBehandling().stream().anyMatch(behandling::harBehandlingÅrsak);
    }

    private boolean skalTilInfoTrygd(BehandlingReferanse ref) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(ref.getSaksnummer()).orElseThrow();
        return fagsak.getSkalTilInfotrygd();
    }
}
