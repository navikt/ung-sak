package no.nav.k9.sak.web.app.tjenester.behandling.sykdom.dokument;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomInnleggelsePeriode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomInnleggelser;


@ApplicationScoped
public class SykdomDokumentOversiktMapper {

    public SykdomDokumentOversikt map(String behandlingUuid, Collection<SykdomDokument> dokumenter) {
        final List<SykdomDokumentOversiktElement> elementer = dokumenter
            .stream()
            .map(d -> {
                return new SykdomDokumentOversiktElement(
                        "" + d.getId(),
                        "1", // TODO: Sett riktig verdi.
                        d.getType(),
                        false,  // TODO: Sette riktig verdi.
                        LocalDate.now(), // TODO: Sette riktig verdi.
                        LocalDate.now(), // TODO: Sette riktig verdi.
                        LocalDateTime.now(), // TODO: Sette riktig verdi.
                        false,  // TODO: Sette riktig verdi.
                        Arrays.asList(
                            linkForGetDokumentinnhold(behandlingUuid, "" + d.getId()),
                            linkForEndreDokument(behandlingUuid, "" + d.getId(), "1") // TODO: Sett riktig verdi på versjon.
                        )
                    );
            })
            .collect(Collectors.toList())
            ;

        return new SykdomDokumentOversikt(
                elementer,
                Arrays.asList()
                );
    }

    private ResourceLink linkForGetDokumentinnhold(String behandlingUuid, String sykdomDokumentId) {
        return ResourceLink.get(BehandlingDtoUtil.getApiPath(SykdomDokumentRestTjeneste.DOKUMENT_INNHOLD_PATH), "sykdom-dokument-innhold", Map.of(BehandlingUuidDto.NAME, behandlingUuid, SykdomDokumentIdDto.NAME, sykdomDokumentId));
    }

    private ResourceLink linkForEndreDokument(String behandlingUuid, String id, String versjon) {
        return ResourceLink.post(BehandlingDtoUtil.getApiPath(SykdomDokumentRestTjeneste.DOKUMENT_PATH), "sykdom-dokument-endring", new SykdomDokumentEndringDto(behandlingUuid, id, versjon));
    }

    public SykdomInnleggelser innleggelseDTOTilSykdomInnleggelser(SykdomInnleggelseDto sykdomInnleggelse, String brukerId) {

        LocalDateTime opprettetTidspunkt = LocalDateTime.now();
        List<SykdomInnleggelsePeriode> perioder = sykdomInnleggelse.getPerioder()
            .stream()
            .map(p -> new SykdomInnleggelsePeriode(null, p.getFom(), p.getTom(), brukerId, opprettetTidspunkt))
            .collect(Collectors.toList());
        SykdomInnleggelser innleggelser = new SykdomInnleggelser(
            Long.parseLong(sykdomInnleggelse.getVersjon()),
            null,
            perioder,
            brukerId,
            opprettetTidspunkt);

        return innleggelser;
    }
}
