package no.nav.ung.sak.ytelseperioder;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Dependent
public class MånedsvisTidslinjeUtleder {

    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;
    private final BehandlingRepository behandlingRepository;
    private final boolean kontrollAvSisteMndEnabled;


    @Inject
    public MånedsvisTidslinjeUtleder(UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste, BehandlingRepository behandlingRepository, @KonfigVerdi(value = "KONTROLL_SISTE_MND_ENABLED", defaultVerdi = "false") boolean kontrollAvSisteMndEnabled) {
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.kontrollAvSisteMndEnabled = kontrollAvSisteMndEnabled;
    }


    /** Utleder oppstykkede ytelseperioder pr måned
     * Ytelseperioder brukes til generering av tilkjent ytelse, rapporteringsperioder for inntekt og eventuelle kontrollperioder for inntekt
     * @param behandlingId Id for behandling
     * @return Oppstykket tidslinje for ytelse
     */
    // Det er litt rart med en tidslinje av periodedata, men det gjøres for å gjøre det veldig tydelig at dette er en tidslinje som ikke skal kunne slås sammen på tvers av måneder
    public LocalDateTimeline<YearMonth> periodiserMånedsvis(Long behandlingId) {
        final var ungdomsprogramperioder = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(behandlingId);
        final var fagsak = behandlingRepository.hentBehandling(behandlingId).getFagsak();
        final var fagsakPeriode = fagsak.getPeriode();
        LocalDateTimeline<Boolean> programOgFagsakTidslinje = ungdomsprogramperioder.intersection(new LocalDateTimeline<>(fagsakPeriode.getFomDato(), fagsakPeriode.getTomDato(), true))
            .compress();
        if (kontrollAvSisteMndEnabled) {
            var mappedSegments = programOgFagsakTidslinje
                .toSegments()
                .stream()
                .map(it -> new LocalDateSegment<>(it.getFom(), it.getTom().with(TemporalAdjusters.lastDayOfMonth()), it.getValue()))
                .toList(); // Mapper segmenter til å dekke hele måneder
            programOgFagsakTidslinje = new LocalDateTimeline<>(mappedSegments, StandardCombinators::alwaysTrueForMatch).compress();
        }
        return programOgFagsakTidslinje
            .splitAtRegular(ungdomsprogramperioder.getMinLocalDate().withDayOfMonth(1), fagsakPeriode.getTomDato(), Period.ofMonths(1))
            .map(it -> List.of(new LocalDateSegment<>(it.getLocalDateInterval(), YearMonth.of(it.getFom().getYear(), it.getFom().getMonthValue()))));
    }


}
