package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;

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
        var utledetTom = finnTomDatoInstans(startdato, originalBehandlingId, startdato);
        return DatoIntervallEntitet.fraOgMedTilOgMed(startdato, utledetTom);
    }

    private LocalDate finnTomDatoInstans(LocalDate søknadFom, Optional<Long> originalBehandlingId, LocalDate fomDato) {
        if (originalBehandlingId.isEmpty()) {
            return fomDato.plusWeeks(52).minusDays(1);
        } else {
            var forrigeBehandlingUngdomsprogramTidslinje = ungdomsprogramPeriodeTjeneste.finnPeriodeTidslinje(originalBehandlingId.get());
            var harForlengetPeriode = ungdomsprogramPeriodeTjeneste.finnHarForlengetPeriode(originalBehandlingId.get());
            var maksDato = ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(originalBehandlingId.get()).orElse(null);
            return finnTomDato(søknadFom, forrigeBehandlingUngdomsprogramTidslinje, harForlengetPeriode, maksDato);
        }
    }

    public static LocalDate finnTomDato(LocalDate søknadFom, LocalDateTimeline<Boolean> ungdomsprogramTidslinje) {
        return finnTomDato(søknadFom, ungdomsprogramTidslinje, false, null);
    }

    public static LocalDate finnTomDato(LocalDate søknadFom, LocalDateTimeline<Boolean> ungdomsprogramTidslinje, boolean harForlengetPeriode) {
        return finnTomDato(søknadFom, ungdomsprogramTidslinje, harForlengetPeriode, null);
    }

    /**
     * Beregner siste dato i fagsakperioden.
     *
     * <p>Hvis {@code periodeMaksDato} er satt, brukes maks-datoen fra registeret direkte
     * (justert til siste virkedag). Dette unngår materialisering av en lukket programperiode.
     * Maks-dato sendes alltid fra ung-deltakelse-opplyser: 260 virkedager ved normal kvote,
     * 300 ved forlenget periode.
     *
     * <p>Ellers beregnes tom-dato fra antall gjenværende virkedager.
     */
    public static LocalDate finnTomDato(LocalDate søknadFom,
                                        LocalDateTimeline<Boolean> ungdomsprogramTidslinje,
                                        boolean harForlengetPeriode,
                                        LocalDate periodeMaksDato) {
        if (periodeMaksDato != null) {
            // Bruk maks-dato direkte fra registeret, justert til siste virkedag.
            return justerTilSisteVirkedag(periodeMaksDato);
        }
        var tidligerePerioderIProgrammet = ungdomsprogramTidslinje.intersection(new LocalDateInterval(LocalDateInterval.TIDENES_BEGYNNELSE, søknadFom.minusDays(1)));
        var vurderAntallDagerResultat = FinnForbrukteDager.finnForbrukteDager(tidligerePerioderIProgrammet, harForlengetPeriode);
        var forbrukteDager = vurderAntallDagerResultat.forbrukteDager();
        var maksAntallDager = FinnForbrukteDager.getMaksAntallDager(harForlengetPeriode);
        if (forbrukteDager >= maksAntallDager) {
            return søknadFom;
        } else {
            var resterendeDager = maksAntallDager - forbrukteDager;
            var weeksToAdd = resterendeDager / VIRKEDAGER_PR_UKE;
            var medHeleAntallUkerLagtTil = søknadFom.plusWeeks(weeksToAdd).minusDays(1);
            var daysToAdd = finnRestDagerÅLeggeTil(medHeleAntallUkerLagtTil, resterendeDager % VIRKEDAGER_PR_UKE);
            return medHeleAntallUkerLagtTil.plusDays(daysToAdd);
        }
    }

    public static LocalDate finnTomDato(UngdomsprogramPeriodeGrunnlag ungdomsprogramPeriodeGrunnlag) {
        LocalDateTimeline<Boolean> periodeTidslinje = UngdomsprogramPeriodeTjeneste.lagPeriodeTidslinje(Optional.of(ungdomsprogramPeriodeGrunnlag));
        return finnTomDato(
            periodeTidslinje.getMinLocalDate(),
            periodeTidslinje,
            ungdomsprogramPeriodeGrunnlag.harForlengetPeriode(),
            ungdomsprogramPeriodeGrunnlag.getPeriodeMaksDato().orElse(null));
    }

    /**
     * Justerer en dato til siste virkedag i samme helg.
     *
     * <p>Brukes for tom-/maksdatoer der perioden ikke skal strekke seg inn i helg.
     * Hvis registeret sender en maksdato på lørdag eller søndag, flyttes datoen
     * tilbake til fredag slik at perioden avsluttes på virkedag.
     *
     * <p>Eksempler:
     * lørdag -> fredag, søndag -> fredag, mandag-fredag -> uendret.
     */
    public static LocalDate justerTilSisteVirkedag(LocalDate dato) {
        return switch (dato.getDayOfWeek()) {
            case SATURDAY -> dato.minusDays(1);
            case SUNDAY -> dato.minusDays(2);
            default -> dato;
        };
    }

    /**
     * Justerer en dato til første virkedag etter helg.
     *
     * <p>Brukes for fom-datoer når en ny periode skal starte "dagen etter"
     * en tidligere tom-/maksdato. Hvis denne dagen lander i helg, flyttes
     * start frem til mandag for å unngå at perioden starter på ikke-virkedag.
     *
     * <p>Eksempler:
     * lørdag -> mandag, søndag -> mandag, mandag-fredag -> uendret.
     */
    public static LocalDate justerTilNesteVirkedag(LocalDate dato) {
        return switch (dato.getDayOfWeek()) {
            case SATURDAY -> dato.plusDays(2);
            case SUNDAY -> dato.plusDays(1);
            default -> dato;
        };
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
