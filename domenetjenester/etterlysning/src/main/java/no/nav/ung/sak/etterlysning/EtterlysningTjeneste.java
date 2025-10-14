package no.nav.ung.sak.etterlysning;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottatteDokumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Dependent
public class EtterlysningTjeneste {

    private Logger log = LoggerFactory.getLogger(EtterlysningTjeneste.class);

    private MottatteDokumentRepository mottatteDokumentRepository;
    private EtterlysningOgUttalelseTjeneste etterlysningOgUttalelseTjeneste;

    @Inject
    public EtterlysningTjeneste(MottatteDokumentRepository mottatteDokumentRepository,
                                EtterlysningOgUttalelseTjeneste etterlysningOgUttalelseTjeneste) {
        this.mottatteDokumentRepository = mottatteDokumentRepository;
        this.etterlysningOgUttalelseTjeneste = etterlysningOgUttalelseTjeneste;
    }


    public LocalDateTimeline<EtterlysningData> hentGjeldendeEtterlysningTidslinje(
        Long behandlingId,
        Long fagsakId,
        EtterlysningType type) {
        final var gjeldendeEtterlysninger = hentGjeldendeEtterlysninger(behandlingId, fagsakId, type);
        return gjeldendeEtterlysninger.stream()
            .map(it -> new LocalDateTimeline<>(it.periode().toLocalDateInterval(), it))
            .reduce(LocalDateTimeline::crossJoin)
            .orElse(LocalDateTimeline.empty());
    }

    /**
     * Henter gjeldende etterlysninger for en behandling og fagsak. Sjekker både VENTER, UTLØPT og MOTTATT_SVAR statuser.
     * Gjeldende etterlysning er den som ble opprettet sist for en periode dersom den ikke er koblet til en uttalelse (statuser VENTER eller UTLØPT)
     * eller den etterlysningern som sist fikk innsendt uttalelse dersom den er koblet til en uttalelse (status MOTTATT_SVAR)
     *
     * @param behandlingId Behandling ID
     * @param fagsakId     Fagsak ID
     * @param type         Type av etterlysning
     * @return Liste med gjeldende etterlysninger
     */
    public List<EtterlysningData> hentGjeldendeEtterlysninger(
        Long behandlingId,
        Long fagsakId,
        EtterlysningType type) {
        final var etterlysninger = etterlysningOgUttalelseTjeneste.hentEtterlysningerOgUttalelser(behandlingId, type).stream().filter(it -> !it.status().equals(EtterlysningStatus.AVBRUTT) && !it.status().equals(EtterlysningStatus.SKAL_AVBRYTES)).toList();
        log.info("Fant {} etterlysninger for behandlingId={} og fagsakId={} av type {}", etterlysninger.size(), behandlingId, fagsakId, type);
        final var journalpostIder = etterlysninger.stream().filter(it -> it.uttalelseData() != null).map(it -> it.uttalelseData().svarJournalpostId()).collect(Collectors.toSet());
        final var mottattDokumenter = mottatteDokumentRepository.hentMottatteDokument(fagsakId, journalpostIder);

        // Ønsker å finne siste gjeldende etterlysning for en periode
        // Ved delvis overlappende etterlysninger finner vi siste blant de som overlapper med minst en annen etterlysning i samme gruppe

        List<EtterlysningData> gjeldendeEtterlysningerListe = new ArrayList<>();

        // Sorter etterlysningene etter periode
        final var sorterteEtterlysninger = etterlysninger.stream()
            .sorted(Comparator.comparing(EtterlysningData::periode))
            .toList();

        // Starter for-løkke med tom tidsline og tom liste med etterlysninger
        List<EtterlysningData> etterlysningerForSammePeriode = new ArrayList<>();
        LocalDateTimeline<Boolean> tidslinjeForSamledeEtterlysninger = LocalDateTimeline.empty();
        for (var etterlysning : sorterteEtterlysninger) {
            log.info("Vurderer etterlysning med periode {}", etterlysning.periode());
            final var overlapperMedSamledeEtterlysninger = !tidslinjeForSamledeEtterlysninger.intersection(etterlysning.periode().toLocalDateInterval()).isEmpty();
            if (overlapperMedSamledeEtterlysninger) {
                log.info("Etterlysning med periode {} overlapper eksisterende periode. Legger til i liste som skal vurderes", etterlysning.periode());

                // Dersom etterlysningen overlapper med eksisterende etterlysninger i tidslinjen legges den til i listen og tidslinjen utvides
                tidslinjeForSamledeEtterlysninger = tidslinjeForSamledeEtterlysninger.crossJoin(new LocalDateTimeline<>(etterlysning.periode().toLocalDateInterval(), true));
                etterlysningerForSammePeriode.add(etterlysning);
            } else {
                log.info("Etterlysning med periode {} overlappet ikke eksisterende periode. Ferdigstiller vurdering av periode før vi starter ny gruppe", etterlysning.periode());

                // Finner gjeldende fra forrige gruppe

                log.info("Finner gjeldende etterlysninger for gruppe: {}", etterlysningerForSammePeriode);
                var gjeldendeForPeriode = finnGjeldendeForPeriode(etterlysningerForSammePeriode, mottattDokumenter);
                if (gjeldendeForPeriode.isPresent()) {
                    log.info("Fant gjeldende etterlysning for periode: {}", gjeldendeForPeriode.get().periode());
                    gjeldendeEtterlysningerListe.add(gjeldendeForPeriode.get());
                } else {
                    if (!etterlysningerForSammePeriode.isEmpty()) {
                        throw new IllegalStateException("Forventet å finne gjeldende etterlysning");
                    }
                }

                // Starter ny gruppe med etterlysninger
                tidslinjeForSamledeEtterlysninger = new LocalDateTimeline<>(etterlysning.periode().toLocalDateInterval(), true);
                log.info("Starter ny gruppe som initialiseres med etterlysning {}", etterlysning);
                etterlysningerForSammePeriode.clear();
                etterlysningerForSammePeriode.add(etterlysning);
            }
        }

        // Finner gjeldende etterlysning for siste gruppe
        log.info("Finner gjeldende for siste gruppe");

        var gjeldendeForPeriode = finnGjeldendeForPeriode(etterlysningerForSammePeriode, mottattDokumenter);
        if (gjeldendeForPeriode.isPresent()) {
            log.info("Fant gjeldende etterlysning for periode: {}", gjeldendeForPeriode.get().periode());
            gjeldendeEtterlysningerListe.add(gjeldendeForPeriode.get());
        } else {
            if (!etterlysningerForSammePeriode.isEmpty()) {
                throw new IllegalStateException("Forventet å finne gjeldende etterlysning");
            }
        }

        return gjeldendeEtterlysningerListe;
    }

    private static Optional<EtterlysningData> finnGjeldendeForPeriode(List<EtterlysningData> etterlysningerForSammePeriode,
                                                                  List<MottattDokument> mottattDokumenter) {
        if (etterlysningerForSammePeriode.isEmpty()) {
            return Optional.empty();
        }

        if (etterlysningerForSammePeriode.stream().filter(it -> it.status().equals(EtterlysningStatus.VENTER)).count() > 1) {
            throw new IllegalStateException("Fant flere etterlysninger på vent for samme periode");
        }

        // Dersom etterlysningen ikke overlapper med eksisterende etterlysninger i tidslinjen har vi en ny gruppe
        final var sisteOpprettetEtterlysningForPeriode = etterlysningerForSammePeriode.stream()
            .max(Comparator.comparing(EtterlysningData::opprettetTidspunkt))
            .orElseThrow(() -> new IllegalStateException("Kunne ikke finne siste opprettede etterlysning"));

        if (sisteOpprettetEtterlysningForPeriode.uttalelseData() == null) {
            return Optional.of(sisteOpprettetEtterlysningForPeriode);
        } else {
            // Bruker innsendt tidspunkt fra mottatt dokument siden dette er mer robust (støtter f.eks. kopi fra original behandling til revurdering)
            return etterlysningerForSammePeriode.stream()
                .filter(it -> it.status().equals(EtterlysningStatus.MOTTATT_SVAR))
                .max(Comparator.comparing(
                    it -> finnInnsendingstidspunkt(it, mottattDokumenter)
                ));
        }
    }

    private static LocalDateTime finnInnsendingstidspunkt(EtterlysningData it, List<MottattDokument> mottattDokumenter) {
        return finnMottattDokument(it, mottattDokumenter).getInnsendingstidspunkt();
    }

    private static MottattDokument finnMottattDokument(EtterlysningData it, List<MottattDokument> mottattDokumenter) {
        return mottattDokumenter.stream().filter(md -> md.getJournalpostId().equals(it.uttalelseData().svarJournalpostId())).findFirst().orElseThrow(() -> new IllegalStateException("Fant ikke mottatt dokument for etterlysning  med periode" + it.periode() + " med journalpostId " + it.uttalelseData().svarJournalpostId()));
    }


}
