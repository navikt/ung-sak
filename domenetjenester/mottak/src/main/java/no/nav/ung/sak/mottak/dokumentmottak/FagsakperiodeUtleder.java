package no.nav.ung.sak.mottak.dokumentmottak;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

import domene.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.behandling.steg.uttak.FinnForbrukteDager;

@Dependent
public class FagsakperiodeUtleder {

    public static final int VIRKEDAGER_PR_UKE = 5;
    private final UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    @Inject
    public FagsakperiodeUtleder(UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste) {
        this.ungdomsprogramPeriodeTjeneste = ungdomsprogramPeriodeTjeneste;
    }


    public DatoIntervallEntitet utledNyPeriodeForFagsak(Behandling behandling, LocalDate søknadFom) {
        var originalBehandlingId = behandling.getOriginalBehandlingId();
        var periode = behandling.getFagsak().getPeriode();
        var fomDato = periode.getFomDato().isBefore(søknadFom) ? periode.getFomDato() : søknadFom;
        var utledetTom = finnTomDato(søknadFom, originalBehandlingId, fomDato);
        var tomDato = periode.getTomDato().isBefore(TIDENES_ENDE) && periode.getTomDato().isAfter(utledetTom) ? periode.getTomDato() : utledetTom;
        return DatoIntervallEntitet.fraOgMedTilOgMed(fomDato, tomDato);
    }

    private LocalDate finnTomDato(LocalDate søknadFom, Optional<Long> originalBehandlingId, LocalDate fomDato) {
        if (originalBehandlingId.isEmpty()) {
            return fomDato.plusWeeks(52).minusDays(1);
        } else {
            var forrigeBehandlingUngdomsprogramTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(originalBehandlingId.get());
            var tidligerePerioderIProgrammet = forrigeBehandlingUngdomsprogramTidslinje.intersection(new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, søknadFom.minusDays(1)));
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
    }

    private long finnRestDagerÅLeggeTil(LocalDate fraDato, long virkedagerSomLeggesTil) {
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
