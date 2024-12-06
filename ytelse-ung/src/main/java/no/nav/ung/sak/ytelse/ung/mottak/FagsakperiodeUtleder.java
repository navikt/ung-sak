package no.nav.ung.sak.ytelse.ung.mottak;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ytelse.ung.periode.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelse.ung.uttak.FinnForbrukteDager;

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
                var medHeleAntallUkerLagtTil = søknadFom.plusWeeks(resterendeDager / VIRKEDAGER_PR_UKE).minusDays(1);
                var daysToAdd = finnRestDagerÅLeggeTil(medHeleAntallUkerLagtTil, resterendeDager % VIRKEDAGER_PR_UKE);
                return medHeleAntallUkerLagtTil.plusDays(daysToAdd);
            }

        }
    }

    private long finnRestDagerÅLeggeTil(LocalDate fra, long modulo) {
        if (modulo >= VIRKEDAGER_PR_UKE) {
            throw new IllegalArgumentException("Forventet mindre enn en uke i resterende dager");
        }
        if (fra.getDayOfWeek().getValue() + modulo < DayOfWeek.FRIDAY.getValue()) {
            return modulo;
        } else {
            return modulo+2;
        }
    }

}
