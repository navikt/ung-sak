package no.nav.ung.sak.ungdomsprogram.forbruktedager;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

@Dependent
public class FagsakperiodeUtleder {

    public static final int VIRKEDAGER_PR_UKE = 5;
    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    @Inject
    public FagsakperiodeUtleder(UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste) {
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
    }


    public DatoIntervallEntitet utledNyPeriodeForFagsak(Behandling behandling, LocalDate startdato) {
        var originalBehandlingId = behandling.getOriginalBehandlingId();
        var utledetTom = finnTomDato(startdato, originalBehandlingId, startdato);
        return DatoIntervallEntitet.fraOgMedTilOgMed(startdato, utledetTom);
    }

    private LocalDate finnTomDato(LocalDate søknadFom, Optional<Long> originalBehandlingId, LocalDate fomDato) {
        if (originalBehandlingId.isEmpty()) {
            return fomDato.plusWeeks(52).minusDays(1);
        } else {
            var forrigeBehandlingUngdomsprogramTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(originalBehandlingId.get());
            return finnTomDato(søknadFom, forrigeBehandlingUngdomsprogramTidslinje);

        }
    }

    public static LocalDate finnTomDato(LocalDate søknadFom, LocalDateTimeline<Boolean> ungdomsprogramTidslinje) {
        var tidligerePerioderIProgrammet = ungdomsprogramTidslinje.intersection(new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, søknadFom.minusDays(1)));
        var vurderAntallDagerResultat = FinnForbrukteDager.finnForbrukteDager(tidligerePerioderIProgrammet);
        var forbrukteDager = vurderAntallDagerResultat.forbrukteDager();
        if (forbrukteDager >= FinnForbrukteDager.MAKS_ANTALL_DAGER) {
            return søknadFom;
        } else {
            var resterendeDager = FinnForbrukteDager.MAKS_ANTALL_DAGER - forbrukteDager;
            var weeksToAdd = resterendeDager / VIRKEDAGER_PR_UKE;
            var medHeleAntallUkerLagtTil = søknadFom.plusWeeks(weeksToAdd).minusDays(1);
            var daysToAdd = finnRestDagerÅLeggeTil(medHeleAntallUkerLagtTil, resterendeDager % VIRKEDAGER_PR_UKE);
            return medHeleAntallUkerLagtTil.plusDays(daysToAdd);
        }
    }

    private static long finnRestDagerÅLeggeTil(LocalDate fraDato, long virkedagerSomLeggesTil) {
        if (virkedagerSomLeggesTil >= VIRKEDAGER_PR_UKE) {
            throw new IllegalArgumentException("Forventet mindre enn en uke i resterende dager");
        }
        if (fraDato.getDayOfWeek().equals(DayOfWeek.SATURDAY)) {
            return virkedagerSomLeggesTil + 1;
        } else if (fraDato.getDayOfWeek().equals(DayOfWeek.SUNDAY) || (fraDato.getDayOfWeek().getValue() + virkedagerSomLeggesTil <= DayOfWeek.FRIDAY.getValue())) {
            return virkedagerSomLeggesTil;
        } else {
            return virkedagerSomLeggesTil + 2;
        }
    }

}
