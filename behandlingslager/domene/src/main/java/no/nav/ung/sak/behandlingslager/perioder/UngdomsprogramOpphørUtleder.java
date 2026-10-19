package no.nav.ung.sak.behandlingslager.perioder;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

/**
 * Samler semantikken for når ungdomsprogramytelsen opphører:
 * <ul>
 *     <li>om programperioden i et periodegrunnlag er lukket eller åpen, og</li>
 *     <li>hvordan programperiodens sluttdato forholder seg til {@code periodeMaksDato}.</li>
 * </ul>
 * {@code periodeMaksDato} er siste dag med ytelse (inklusiv). Programperioden er typisk åpen
 * ({@code tom = TIDENES_ENDE}), så sluttdatoen i registeret alene sier ikke når ytelsen opphører.
 * <p>
 * Kallestedet henter selv grunnlaget det er interessert i - for behandlingen selv eller for originalbehandlingen.
 */
public final class UngdomsprogramOpphørUtleder {

    private UngdomsprogramOpphørUtleder() {
    }

    /**
     * @return siste dag med ytelse for en programperiode: den tidligste av programperiodens {@code tom} og maksdato.
     */
    public static LocalDate finnSisteDagMedYtelse(LocalDate programperiodeTom, LocalDate maksdato) {
        return programperiodeTom.isBefore(maksdato) ? programperiodeTom : maksdato;
    }

    /**
     * @return true dersom programperioden varer minst til og med maksdato, slik at det er maksdato (og ikke en
     * tidligere sluttdato) som avslutter ytelsen. Da varsles deltaker via varsel om opphør ved maksdato.
     */
    public static boolean opphørerVedMaksdato(LocalDate programperiodeTom, LocalDate maksdato) {
        return !programperiodeTom.isBefore(maksdato);
    }

    /**
     * @return true dersom datoen ligger etter maksdato, dvs. utenfor perioden det kan gis ytelse for.
     */
    public static boolean erEtterMaksdato(LocalDate dato, LocalDate maksdato) {
        return dato.isAfter(maksdato);
    }

    /**
     * @return sluttdatoen dersom programperioden er lukket, ellers tom — åpen sluttdato betyr løpende program.
     */
    public static Optional<LocalDate> finnLukketSluttdato(UngdomsprogramPeriodeGrunnlag grunnlag) {
        var perioder = hentPerioder(grunnlag);
        if (!harLukketSluttdato(perioder)) {
            return Optional.empty();
        }
        return perioder.stream().map(it -> it.getPeriode().getTomDato()).max(LocalDate::compareTo);
    }

    public static boolean harLukketSluttdato(UngdomsprogramPeriodeGrunnlag grunnlag) {
        return harLukketSluttdato(hentPerioder(grunnlag));
    }

    /**
     * Merk at dette <em>ikke</em> er negasjonen av {@link #harLukketSluttdato}: et grunnlag uten perioder er hverken
     * lukket eller åpent.
     */
    public static boolean harÅpenSluttdato(UngdomsprogramPeriodeGrunnlag grunnlag) {
        return hentPerioder(grunnlag).stream().anyMatch(UngdomsprogramOpphørUtleder::erÅpen);
    }

    private static boolean harLukketSluttdato(Set<UngdomsprogramPeriode> perioder) {
        return !perioder.isEmpty() && perioder.stream().noneMatch(UngdomsprogramOpphørUtleder::erÅpen);
    }

    private static boolean erÅpen(UngdomsprogramPeriode periode) {
        return Tid.TIDENES_ENDE.equals(periode.getPeriode().getTomDato());
    }

    private static Set<UngdomsprogramPeriode> hentPerioder(UngdomsprogramPeriodeGrunnlag grunnlag) {
        return grunnlag == null || grunnlag.getUngdomsprogramPerioder() == null
            ? Set.of()
            : grunnlag.getUngdomsprogramPerioder().getPerioder();
    }

}
