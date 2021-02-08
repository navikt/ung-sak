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

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.kontrakt.ResourceLink;
import no.nav.k9.sak.kontrakt.behandling.BehandlingUuidDto;
import no.nav.k9.sak.typer.Periode;
import no.nav.k9.sak.web.app.tjenester.behandling.BehandlingDtoUtil;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDiagnosekode;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDiagnosekoder;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokument;
import no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom.SykdomDokumentType;
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
                        d.getType() != SykdomDokumentType.UKLASSIFISERT,  // TODO: Sette riktig verdi.
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

    public SykdomDiagnosekoder toSykdomDiagnosekoder(SykdomDiagnosekoderDto dto, String brukerId ) {
        LocalDateTime opprettetTidspunkt = LocalDateTime.now();
        List<SykdomDiagnosekode> kodeliste = dto.getDiagnosekoder()
            .stream()
            .map(k -> new SykdomDiagnosekode(k.getVerdi(), brukerId, opprettetTidspunkt))
            .collect(Collectors.toCollection(ArrayList::new));

        return new SykdomDiagnosekoder(
            dto.getVersjon() != null ? Long.valueOf(dto.getVersjon()) : null,
            null,
            kodeliste,
            brukerId,
            opprettetTidspunkt);
    }

    public SykdomDiagnosekoderDto toSykdomDiagnosekoderDto(SykdomDiagnosekoder diagnosekoder, Behandling behandling) {
        final var endreDiagnosekoderLink = ResourceLink.post(BehandlingDtoUtil.getApiPath(SykdomDokumentRestTjeneste.SYKDOM_DIAGNOSEKODER_PATH), "sykdom-diagnosekoder-endring", new SykdomDiagnosekoderDto(behandling.getUuid().toString()));
        return new SykdomDiagnosekoderDto(
            behandling.getUuid(),
            (diagnosekoder.getVersjon() != null) ? diagnosekoder.getVersjon().toString() : null,
            diagnosekoder.getDiagnosekoder()
                .stream()
                .map(
                    k -> new SykdomDiagnosekodeDto(k.getDiagnosekode()))
                .collect(Collectors.toList()),
            Arrays.asList(endreDiagnosekoderLink));
    }

    public SykdomInnleggelser toSykdomInnleggelser(SykdomInnleggelseDto sykdomInnleggelse, String brukerId) {

        LocalDateTime opprettetTidspunkt = LocalDateTime.now();
        List<SykdomInnleggelsePeriode> perioder = sykdomInnleggelse.getPerioder()
            .stream()
            .map(p -> new SykdomInnleggelsePeriode(null, p.getFom(), p.getTom(), brukerId, opprettetTidspunkt))
            .collect(Collectors.toCollection(ArrayList::new));
        return new SykdomInnleggelser(
            (sykdomInnleggelse.getVersjon() != null) ? Long.valueOf(sykdomInnleggelse.getVersjon()) : null,
            null,
            perioder,
            brukerId,
            opprettetTidspunkt);
    }

    public SykdomInnleggelseDto toSykdomInnleggelseDto(SykdomInnleggelser innleggelser, Behandling behandling) {
        return new SykdomInnleggelseDto(
            behandling.getUuid(),
            (innleggelser.getVersjon() != null) ? innleggelser.getVersjon().toString() : null,
            innleggelser.getPerioder()
                .stream()
                .map(
                    p -> new Periode(p.getFom(), p.getTom()))
                .collect(Collectors.toList()),
            Arrays.asList(ResourceLink.post(
                BehandlingDtoUtil.getApiPath(SykdomDokumentRestTjeneste.SYKDOM_INNLEGGELSE_PATH),
                "sykdom-innleggelse-endring",
                new SykdomInnleggelseDto(behandling.getUuid().toString()))));
    }
}
