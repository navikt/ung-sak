package no.nav.k9.sak.mottak.forsendelse.tjeneste;

import java.util.List;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.MottattDokument;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.MottatteDokumentRepository;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.sak.kontrakt.mottak.ForsendelseIdDto;
import no.nav.k9.sak.kontrakt.mottak.ForsendelseStatus;
import no.nav.k9.sak.kontrakt.mottak.ForsendelseStatusData;
import no.nav.k9.sak.typer.JournalpostId;

@ApplicationScoped
public class ForsendelseStatusTjeneste {

    private MottatteDokumentRepository mottatteDokumentRepository;

    private BehandlingRepository behandlingRepository;

    public ForsendelseStatusTjeneste() {
        // FOR CDI
    }

    @Inject
    public ForsendelseStatusTjeneste(MottatteDokumentRepository mottatteDokumentRepository, BehandlingRepository behandlingRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.behandlingRepository = behandlingRepository;
    }

    public ForsendelseStatusData getStatusInformasjon(ForsendelseIdDto forsendelseIdDto) {
        UUID forsendelseId = forsendelseIdDto.getForsendelseId();
        List<MottattDokument> mottattDokumentList = mottatteDokumentRepository.hentMottatteDokumentMedForsendelseId(forsendelseId);

        if (mottattDokumentList == null || mottattDokumentList.isEmpty()) {
            throw ForsendelseStatusFeil.FACTORY.finnesIkkeMottatDokument(forsendelseId).toException();
        } else if (mottattDokumentList.size() != 1) {
            throw ForsendelseStatusFeil.FACTORY.flereMotattDokument(forsendelseId).toException();
        }
        MottattDokument mottattDokument = mottattDokumentList.get(0);
        Behandling behandling = behandlingRepository.hentBehandling(mottattDokument.getBehandlingId());
        ForsendelseStatusData forsendelseStatusDataDTO = getForsendelseStatusDataDTO(behandling, forsendelseId, mottattDokument.getJournalpostId());
        return forsendelseStatusDataDTO;
    }

    private ForsendelseStatusData getForsendelseStatusDataDTO(Behandling behandling, UUID forsendelseId, JournalpostId journalpostId) {
        ForsendelseStatusData dto;
        if (behandling.erStatusFerdigbehandlet()) {
            BehandlingResultatType resultat = behandling.getBehandlingResultatType();
            if(resultat.equals(BehandlingResultatType.INNVILGET)) {
                dto = new ForsendelseStatusData(ForsendelseStatus.INNVILGET);
            } else if(resultat.equals(BehandlingResultatType.AVSLÅTT)) {
                dto = new ForsendelseStatusData(ForsendelseStatus.AVSLÅTT);
            } else {
                throw ForsendelseStatusFeil.FACTORY.ugyldigBehandlingResultat(forsendelseId).toException();
            }

        } else {
            List<Aksjonspunkt> aksjonspunkt = behandling.getÅpneAksjonspunkter();
            if (aksjonspunkt.isEmpty()) {
                dto = new ForsendelseStatusData(ForsendelseStatus.PÅGÅR);
            } else {
                dto = new ForsendelseStatusData(ForsendelseStatus.PÅ_VENT);
            }
        }
        
        dto.setSaksnummer(behandling.getFagsak().getSaksnummer());
        dto.setJournalpostId(journalpostId);
        
        return dto;
    }

}
