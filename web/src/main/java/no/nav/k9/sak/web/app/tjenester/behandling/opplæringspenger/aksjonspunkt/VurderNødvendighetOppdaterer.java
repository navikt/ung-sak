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
import no.nav.k9.sak.kontrakt.opplæringspenger.VurderNødvendighetDto;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæring;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringGrunnlag;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringHolder;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.VurdertOpplæringRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.pleietrengendesykdom.PleietrengendeSykdomDokumentRepository;
import no.nav.k9.sikkerhet.context.SubjectHandler;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderNødvendighetDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderNødvendighetOppdaterer implements AksjonspunktOppdaterer<VurderNødvendighetDto> {

    private VurdertOpplæringRepository vurdertOpplæringRepository;
    private BehandlingRepository behandlingRepository;
    private PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository;

    public VurderNødvendighetOppdaterer() {
    }

    @Inject
    public VurderNødvendighetOppdaterer(VurdertOpplæringRepository vurdertOpplæringRepository,
                                        BehandlingRepository behandlingRepository,
                                        PleietrengendeSykdomDokumentRepository pleietrengendeSykdomDokumentRepository) {
        this.vurdertOpplæringRepository = vurdertOpplæringRepository;
        this.behandlingRepository = behandlingRepository;
        this.pleietrengendeSykdomDokumentRepository = pleietrengendeSykdomDokumentRepository;
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
        final List<PleietrengendeSykdomDokument> alleDokumenter = pleietrengendeSykdomDokumentRepository.hentAlleDokumenterFor(behandling.getFagsak().getPleietrengendeAktørId());

        VurdertOpplæring nyVurdertOpplæring = mapDtoTilVurdertOpplæring(dto, alleDokumenter);
        vurdertOpplæring.add(nyVurdertOpplæring);

        VurdertOpplæringHolder nyHolder = new VurdertOpplæringHolder(vurdertOpplæring);

        vurdertOpplæringRepository.lagre(param.getBehandlingId(), nyHolder);
        return OppdateringResultat.nyttResultat();
    }

    private VurdertOpplæring mapDtoTilVurdertOpplæring(VurderNødvendighetDto dto, List<PleietrengendeSykdomDokument> alleDokumenter) {
        return new VurdertOpplæring(dto.getJournalpostId().getJournalpostId(), dto.isNødvendigOpplæring(), dto.getBegrunnelse(), getCurrentUserId(), LocalDateTime.now(),
            alleDokumenter.stream().filter(dokument -> dto.getTilknyttedeDokumenter().contains("" + dokument.getId())).collect(Collectors.toList()));
    }

    private static String getCurrentUserId() {
        return SubjectHandler.getSubjectHandler().getUid();
    }
}
