package no.nav.k9.sak.ytelse.frisinn.brev;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vedtak.Vedtaksbrev;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarsel;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.dokument.bestill.DokumentBestillerApplikasjonTjeneste;
import no.nav.k9.sak.domene.vedtak.intern.SendVedtaksbrev;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FagsakYtelseTypeRef("FRISINN")
@ApplicationScoped
public class SendVedtaksbrevFrisinn extends SendVedtaksbrev {

    private static final Logger log = LoggerFactory.getLogger(SendVedtaksbrev.class);

    private VedtakVarselRepository vedtakvarselRepository;

    public SendVedtaksbrevFrisinn() {
        // for proxy
    }

    @Inject
    public SendVedtaksbrevFrisinn(BehandlingRepository behandlingRepository,
                           FagsakRepository fagsakRepository,
                           BehandlingVedtakRepository behandlingVedtakRepository,
                           VedtakVarselRepository vedtakvarselRepository,
                           DokumentBestillerApplikasjonTjeneste dokumentBestillerApplikasjonTjeneste) {
        super(behandlingRepository, fagsakRepository, behandlingVedtakRepository, vedtakvarselRepository, dokumentBestillerApplikasjonTjeneste);
        this.vedtakvarselRepository = vedtakvarselRepository;
    }

    @Override
    protected boolean skalSendeVedtaksbrev(BehandlingReferanse ref) {
        if (!super.skalSendeVedtaksbrev(ref)) {
            return false;
        }

        if (erUndertryktBrevFrisinn(ref)) {
            log.info("Vedtaksbrev for frisinn overstyrt og sendes derfor ikke automatisk for behandling {}", ref.getBehandlingId()); //$NON-NLS-1$
            return false;
        }

        if (ref.getBehandlingResultat().isBehandlingsresultatIkkeEndret()) {
            log.info("Sender ikke vedtaksbrev for frisinn ved revurdering som ikke fører til endring i utbetaling. BehandlingId {}", ref.getBehandlingId()); //$NON-NLS-1$
            return false;
        }

        return true;
    }


    private boolean erUndertryktBrevFrisinn(BehandlingReferanse ref) {
        boolean ingenVedtaksbrev = vedtakvarselRepository.hentHvisEksisterer(ref.getBehandlingId())
            .map(VedtakVarsel::getVedtaksbrev)
            .map(Vedtaksbrev.INGEN::equals)
            .orElse(false);

        return ref.getFagsakYtelseType() == FagsakYtelseType.FRISINN && ingenVedtaksbrev;
    }
}
