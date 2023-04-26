package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.aksjonspunkt;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.k9.sak.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.kontrakt.opplæringspenger.vurdering.VurderNødvendighetDto;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokument;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokumentRepository;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.vurdering.VurdertOpplæringRepository;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderNødvendighetDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderNødvendighetOppdaterer implements AksjonspunktOppdaterer<VurderNødvendighetDto> {

    private VurdertOpplæringRepository vurdertOpplæringRepository;
    private BehandlingRepository behandlingRepository;
    private OpplæringDokumentRepository opplæringDokumentRepository;

    public VurderNødvendighetOppdaterer() {
    }

    @Inject
    public VurderNødvendighetOppdaterer(VurdertOpplæringRepository vurdertOpplæringRepository,
                                        BehandlingRepository behandlingRepository,
                                        OpplæringDokumentRepository opplæringDokumentRepository) {
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
        this.behandlingRepository = behandlingRepository;
        this.opplæringDokumentRepository = opplæringDokumentRepository;
    }

    @Override
    public OppdateringResultat oppdater(VurderNødvendighetDto dto, AksjonspunktOppdaterParameter param) {
        List<VurdertOpplæring> vurdertOpplæring = new ArrayList<>();

        Optional<VurdertOpplæringHolder> aktivVurdertOpplæringHolder = vurdertOpplæringRepository.hentAktivtGrunnlagForBehandling(param.getBehandlingId())
            .map(VurdertOpplæringGrunnlag::getVurdertOpplæringHolder);

        if (aktivVurdertOpplæringHolder.isPresent()) {
            List<VurdertOpplæring> aktiveVurdertInstitusjoner = aktivVurdertOpplæringHolder.get().getVurdertOpplæring().stream()
                .filter(aktivVurdertOpplæring -> !aktivVurdertOpplæring.getJournalpostId().equals(dto.getJournalpostId().getJournalpostId()))
                .toList();
            vurdertOpplæring.addAll(aktiveVurdertInstitusjoner);
        }

        final Behandling behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        final List<OpplæringDokument> alleDokumenter = opplæringDokumentRepository.hentDokumenterForSak(behandling.getFagsak().getId());

        VurdertOpplæring nyVurdertOpplæring = mapDtoTilVurdertOpplæring(dto, alleDokumenter);
        vurdertOpplæring.add(nyVurdertOpplæring);

        VurdertOpplæringHolder nyHolder = new VurdertOpplæringHolder(vurdertOpplæring);

        vurdertOpplæringRepository.lagre(param.getBehandlingId(), nyHolder);
        return OppdateringResultat.nyttResultat();
    }

    private VurdertOpplæring mapDtoTilVurdertOpplæring(VurderNødvendighetDto dto, List<OpplæringDokument> alleDokumenter) {
        return new VurdertOpplæring(dto.getJournalpostId().getJournalpostId(), dto.isNødvendigOpplæring(), dto.getBegrunnelse(), getCurrentUserId(), LocalDateTime.now(),
            alleDokumenter.stream().filter(dokument -> dto.getTilknyttedeDokumenter().contains("" + dokument.getId())).collect(Collectors.toList()));
    }

    private static String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }
}
