package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.etterlysning.EtterlysningStatus;
import no.nav.ung.kodeverk.etterlysning.EtterlysningType;
import no.nav.ung.sak.behandlingslager.BaseEntitet;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.UttalelseEntitet;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Dependent
public class EtterlysningTjeneste {

    private MottatteDokumentRepository mottatteDokumentRepository;
    private EtterlysningRepository etterlysningRepository;

    @Inject
    public EtterlysningTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                EtterlysningRepository etterlysningRepository) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.etterlysningRepository = etterlysningRepository;
    }


    public LocalDateTimeline<EtterlysningData> hentGjeldendeEtterlysningTidslinje(
            Long behandlingId,
            Long fagsakId,
            EtterlysningType type) {
        final var gjeldendeEtterlysninger = hentGjeldendeEtterlysninger(behandlingId, fagsakId, type);
        return gjeldendeEtterlysninger.stream()
                .map(hen -> new LocalDateTimeline<>(hen.getPeriode().toLocalDateInterval(), new EtterlysningData(hen.getStatus(), hen.getFrist(), hen.getGrunnlagsreferanse(), hen.getUttalelse().map(ut -> new UttalelseData(ut.harGodtattEndringen(), ut.getUttalelseBegrunnelse())).orElse(null))))
                .reduce(LocalDateTimeline::crossJoin)
                .orElse(LocalDateTimeline.empty());
    }

    /**
     * Henter gjeldende etterlysninger for en behandling og fagsak.
     * Gjeldende etterlysning er den som ble opprettet sist for en periode dersom den ikke er koblet til en uttalelse (statuser VENTER eller UTLØPT)
     * eller den etterlysningern som sist fikk innsendt uttalelse dersom den er koblet til en uttalelse (status MOTTATT_SVAR)
     *
     * @param behandlingId Behandling ID
     * @param fagsakId     Fagsak ID
     * @param type         Type av etterlysning
     * @return Liste med gjeldende etterlysninger
     */
    public List<Etterlysning> hentGjeldendeEtterlysninger(
            Long behandlingId,
            Long fagsakId,
            EtterlysningType type) {
        final var etterlysninger = etterlysningRepository.hentEtterlysninger(behandlingId, type);
        final var journalpostIder = etterlysninger.stream().flatMap(it -> it.getUttalelse().stream().map(UttalelseEntitet::getSvarJournalpostId)).collect(Collectors.toSet());
        final var mottattDokumenter = mottatteDokumentRepository.hentMottatteDokument(fagsakId, journalpostIder);

        // Ønsker å finne siste gjeldende etterlysning for en periode
        // Ved delvis overlappende etterlysninger finner vi siste blant de som overlapper med minst en annen etterlysning i samme gruppe

        List<Etterlysning> gjeldendeEtterlysningerListe = new ArrayList<>();

        // Sorter etterlysningene etter periode
        final var sorterteEtterlysninger = etterlysninger.stream()
                .sorted(Comparator.comparing(Etterlysning::getPeriode))
                .toList();

        // Starter for-løkke med tom tidsline og tom liste med etterlysninger
        List<Etterlysning> etterlysningerForSammePeriode = new ArrayList<>();
        LocalDateTimeline<Boolean> tidslinjeForSamledeEtterlysninger = LocalDateTimeline.empty();
        for (var etterlysning : sorterteEtterlysninger) {
            final var overlapperMedSamledeEtterlysninger = !tidslinjeForSamledeEtterlysninger.intersection(etterlysning.getPeriode().toLocalDateInterval()).isEmpty();
            if (overlapperMedSamledeEtterlysninger) {
                // Dersom etterlysningen overlapper med eksisterende etterlysninger i tidslinjen legges den til i listen og tidslinjen utvides
                tidslinjeForSamledeEtterlysninger = tidslinjeForSamledeEtterlysninger.crossJoin(new LocalDateTimeline<>(etterlysning.getPeriode().toLocalDateInterval(), true));
                etterlysningerForSammePeriode.add(etterlysning);
            } else {
                // Finner gjeldende fra forrige gruppe
                finnGjeldendeForPeriode(etterlysningerForSammePeriode, mottattDokumenter)
                        .ifPresent(gjeldendeEtterlysningerListe::add);

                // Starter ny gruppe med etterlysninger
                tidslinjeForSamledeEtterlysninger = new LocalDateTimeline<>(etterlysning.getPeriode().toLocalDateInterval(), true);
                etterlysningerForSammePeriode.clear();
                etterlysningerForSammePeriode.add(etterlysning);
            }
        }

        // Finner gjeldende etterlysning for siste gruppe
        finnGjeldendeForPeriode(etterlysningerForSammePeriode, mottattDokumenter)
                .ifPresent(gjeldendeEtterlysningerListe::add);


        return gjeldendeEtterlysningerListe;
    }

    private static Optional<Etterlysning> finnGjeldendeForPeriode(List<Etterlysning> etterlysningerForSammePeriode,
                                                                  List<MottattDokument> mottattDokumenter) {
        if (etterlysningerForSammePeriode.isEmpty()) {
            return Optional.empty();
        }

        if (etterlysningerForSammePeriode.stream().filter(it -> it.getStatus().equals(EtterlysningStatus.VENTER)).count() > 1) {
            throw new IllegalStateException("Fant flere etterlysninger på vent for samme periode");
        }

        // Dersom etterlysningen ikke overlapper med eksisterende etterlysninger i tidslinjen har vi en ny gruppe
        final var sisteOpprettetEtterlysningForPeriode = etterlysningerForSammePeriode.stream()
                .max(Comparator.comparing(BaseEntitet::getOpprettetTidspunkt))
                .orElseThrow(() -> new IllegalStateException("Kunne ikke finne siste opprettede etterlysning"));

        if (sisteOpprettetEtterlysningForPeriode.getUttalelse().isEmpty()) {
            return Optional.of(sisteOpprettetEtterlysningForPeriode);
        } else {
            // Bruker innsendt tidspunkt fra mottatt dokument siden dette er mer robust (støtter f.eks. kopi fra original behandling til revurdering)
            return etterlysningerForSammePeriode.stream()
                    .filter(it -> it.getStatus().equals(EtterlysningStatus.MOTTATT_SVAR))
                    .max(Comparator.comparing(
                            it -> finnInnsendingstidspunkt(it, mottattDokumenter)
                    ));
        }
    }

    private static LocalDateTime finnInnsendingstidspunkt(Etterlysning it, List<MottattDokument> mottattDokumenter) {
        return finnMottattDokument(it, mottattDokumenter).getInnsendingstidspunkt();
    }

    private static MottattDokument finnMottattDokument(Etterlysning it, List<MottattDokument> mottattDokumenter) {
        return mottattDokumenter.stream().filter(md -> md.getJournalpostId().equals(it.getUttalelse().get().getSvarJournalpostId())).findFirst().orElseThrow(() -> new IllegalStateException("Fant ikke mottatt dokument for etterlysning " + it.getId() + " med journalpostId " + it.getUttalelse().get().getSvarJournalpostId()));
    }


}
