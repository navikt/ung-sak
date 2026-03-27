package no.nav.ung.sak.ytelseperioder;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;

import java.time.Period;
import java.time.YearMonth;
import java.util.List;

@Dependent
public class MånedsvisTidslinjeUtleder {

    private final Instance<KvalifiserteYtelsesperioderTjeneste> periodeTjenester;
    private final BehandlingRepository behandlingRepository;


    @Inject
    public MånedsvisTidslinjeUtleder(@Any Instance<KvalifiserteYtelsesperioderTjeneste> periodeTjenester, BehandlingRepository behandlingRepository) {
        this.periodeTjenester = periodeTjenester;
        this.behandlingRepository = behandlingRepository;
    }


    /** Utleder oppstykkede ytelseperioder pr måned
     * Ytelseperioder brukes til generering av tilkjent ytelse, rapporteringsperioder for inntekt og eventuelle kontrollperioder for inntekt
     * @param behandlingId Id for behandling
     * @return Oppstykket tidslinje for ytelse
     */
    // Det er litt rart med en tidslinje av periodedata, men det gjøres for å gjøre det veldig tydelig at dette er en tidslinje som ikke skal kunne slås sammen på tvers av måneder
    public LocalDateTimeline<YearMonth> finnMånedsvisPeriodisertePerioder(Long behandlingId) {
        final var fagsak = behandlingRepository.hentBehandling(behandlingId).getFagsak();
        final var ungdomsprogramperioder = KvalifiserteYtelsesperioderTjeneste.finnTjeneste(fagsak.getYtelseType(), periodeTjenester).finnPeriodeTidslinje(behandlingId);
        return finnMånedsvisPeriodisertePerioder(fagsak, ungdomsprogramperioder);
    }

    /** Utleder initielle oppstykkede ytelseperioder pr måned
     * Ytelseperioder brukes til generering av tilkjent ytelse, rapporteringsperioder for inntekt og eventuelle kontrollperioder for inntekt
     * @param behandlingId Id for behandling
     * @return Oppstykket tidslinje for ytelse
     */
    // Det er litt rart med en tidslinje av periodedata, men det gjøres for å gjøre det veldig tydelig at dette er en tidslinje som ikke skal kunne slås sammen på tvers av måneder
    public LocalDateTimeline<YearMonth> finnInitielleMånedsvisPeriodisertePerioder(Long behandlingId) {
        final var fagsak = behandlingRepository.hentBehandling(behandlingId).getFagsak();
        final var ungdomsprogramperioder = KvalifiserteYtelsesperioderTjeneste.finnTjeneste(fagsak.getYtelseType(), periodeTjenester).finnInitiellPeriodeTidslinje(behandlingId);
        return finnMånedsvisPeriodisertePerioder(fagsak, ungdomsprogramperioder);
    }

    public static LocalDateTimeline<YearMonth> finnMånedsvisPeriodisertePerioder(Fagsak fagsak, LocalDateTimeline<Boolean> perioder) {
        final var fagsakPeriode = fagsak.getPeriode();
        LocalDateTimeline<Boolean> programOgFagsakTidslinje = perioder.intersection(new LocalDateTimeline<>(fagsakPeriode.getFomDato(), fagsakPeriode.getTomDato(), true))
            .compress();
        if (programOgFagsakTidslinje.isEmpty()) {
            return LocalDateTimeline.empty();
        }
        return programOgFagsakTidslinje
            .splitAtRegular(perioder.getMinLocalDate().withDayOfMonth(1), fagsakPeriode.getTomDato(), Period.ofMonths(1))
            .map(it -> List.of(new LocalDateSegment<>(it.getLocalDateInterval(), YearMonth.of(it.getFom().getYear(), it.getFom().getMonthValue()))));
    }


}
