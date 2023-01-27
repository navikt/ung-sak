package no.nav.k9.sak.web.app.tjenester.behandling.opplæringspenger.dokument;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.kontrakt.opplæringspenger.dokument.OpplæringDokumentDto;
import no.nav.k9.sak.kontrakt.sykdom.dokument.SykdomDokumentIdDto;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokument;
import no.nav.k9.sak.ytelse.opplaeringspenger.repo.dokument.OpplæringDokumentRepository;

@Dependent
public class OpplæringDokumentMapper {

    private final OpplæringDokumentRepository opplæringDokumentRepository;

    @Inject
    public OpplæringDokumentMapper(OpplæringDokumentRepository opplæringDokumentRepository) {
        this.opplæringDokumentRepository = opplæringDokumentRepository;
    }

    public List<OpplæringDokumentDto> mapDokumenter(UUID behandlingUuid, List<OpplæringDokument> dokumenter) {
        return dokumenter.stream()
            .map(d -> new OpplæringDokumentDto(
                "" + d.getId(),
                d.getType(),
                d.getDatert(),
                behandlingUuid.equals(d.getSøkersBehandlingUuid()),
                opplæringDokumentRepository.isDokumentBruktIVurdering(d.getId()),
                List.of(linkForGetDokumentinnhold(behandlingUuid.toString(), "" + d.getId()))))
            .collect(Collectors.toList());
    }

    private ResourceLink linkForGetDokumentinnhold(String behandlingUuid, String dokumentId) {
        return ResourceLink.get(BehandlingDtoUtil.getApiPath(OpplæringDokumentRestTjeneste.DOKUMENT_INNHOLD_PATH), "opplæring-dokument-innhold", Map.of(BehandlingUuidDto.NAME, behandlingUuid, SykdomDokumentIdDto.NAME, dokumentId));
    }
}
