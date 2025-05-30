package no.nav.ung.sak.historikk;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagDokumentLink;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinje;
import no.nav.ung.sak.behandlingslager.behandling.historikk.HistorikkinnslagLinjeType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.kontrakt.historikk.HistorikkInnslagDokumentLinkDto;
import no.nav.ung.sak.kontrakt.historikk.HistorikkinnslagDto;
import no.nav.ung.sak.typer.JournalpostId;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Dependent
public class HistorikkInnslagKonverter {

    private BehandlingRepository behandlingRepository;

    @Inject
    public HistorikkInnslagKonverter(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    public HistorikkinnslagDto map(Historikkinnslag h, List<JournalpostId> journalPosterForSak) {
        var behandlingId = h.getBehandlingId();
        var uuid = behandlingId == null ? null : behandlingRepository.hentBehandling(behandlingId).getUuid();
        List<HistorikkInnslagDokumentLinkDto> dokumenter = tilDokumentlenker(h.getDokumentLinker(), journalPosterForSak);
        var linjer = h.getLinjer()
            .stream()
            .sorted(Comparator.comparing(HistorikkinnslagLinje::getSekvensNr))
            .map(t -> t.getType() == HistorikkinnslagLinjeType.TEKST ? HistorikkinnslagDto.Linje.tekstlinje(t.getTekst()) : HistorikkinnslagDto.Linje.linjeskift())
            .toList();
        return new HistorikkinnslagDto(uuid, HistorikkinnslagDto.HistorikkAktørDto.fra(h.getAktør(), h.getOpprettetAv()), h.getSkjermlenke(),
            h.getOpprettetTidspunkt(), dokumenter, h.getTittel(), linjer);
    }

    private static List<HistorikkInnslagDokumentLinkDto> tilDokumentlenker(List<HistorikkinnslagDokumentLink> dokumentLinker,
                                                                           List<JournalpostId> journalPosterForSak) {
        if (dokumentLinker == null) {
            return List.of();
        }
        return dokumentLinker.stream().map(d -> tilDokumentlenke(d, journalPosterForSak)) //
            .toList();
    }

    private static HistorikkInnslagDokumentLinkDto tilDokumentlenke(HistorikkinnslagDokumentLink lenke,
                                                                    List<JournalpostId> journalPosterForSak) {
        var erUtgått = aktivJournalPost(lenke.getJournalpostId(), journalPosterForSak);
        var dto = new HistorikkInnslagDokumentLinkDto();
        dto.setTag(erUtgått ? String.format("%s (utgått)", lenke.getLinkTekst()) : lenke.getLinkTekst());
        dto.setUtgått(erUtgått);
        dto.setDokumentId(lenke.getDokumentId());
        dto.setJournalpostId(lenke.getJournalpostId().getVerdi());
        return dto;
    }

    private static boolean aktivJournalPost(JournalpostId journalpostId, List<JournalpostId> journalPosterForSak) {
        return journalPosterForSak.stream().filter(ajp -> Objects.equals(ajp, journalpostId)).findFirst().isEmpty();
    }

}
