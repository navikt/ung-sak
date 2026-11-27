package no.nav.ung.sak.behandlingslager.perioder;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Samler semantikken for når ungdomsprogramytelsen opphører:
 * <ul>
 *     <li>om programperioden i et periodegrunnlag er lukket eller åpen, og</li>
 *     <li>hvordan programperiodens sluttdato forholder seg til {@code periodeMaksDato}.</li>
 * </ul>
 * {@code periodeMaksDato} er siste dag med ytelse (inklusiv). Programperioden er typisk åpen
 * ({@code tom = TIDENES_ENDE}), så sluttdatoen i registeret alene sier ikke når ytelsen opphører.
 */
public final class UngdomsprogramOpphørUtleder {

    private UngdomsprogramOpphørUtleder() {
    }

    /**
     * @return siste dag med ytelse: den tidligste av programperiodens {@code tom} og maksdato.
     * @throws IllegalStateException dersom grunnlaget mangler maksdato eller ikke har nøyaktig én programperiode
     */
    public static LocalDate finnSisteDagMedYtelse(UngdomsprogramPeriodeGrunnlag grunnlag) {
        var maksdato = hentMaksdato(grunnlag);
        return finnSisteDagMedYtelse(grunnlag.hentForEksaktEnPeriode().getTomDato(), maksdato);
    }

    /**
     * @return true dersom programperioden varer minst til og med maksdato, slik at det er maksdato (og ikke en
     * tidligere sluttdato) som avslutter ytelsen.
     * @throws IllegalStateException dersom grunnlaget mangler maksdato eller ikke har nøyaktig én programperiode
     */
    public static boolean opphørerVedMaksdato(UngdomsprogramPeriodeGrunnlag grunnlag) {
        var maksdato = hentMaksdato(grunnlag);
        return !grunnlag.hentForEksaktEnPeriode().getTomDato().isBefore(maksdato);
    }

    /**
     * @return programperiodene i grunnlaget med {@code tom} kappet mot maksdato. Perioder som starter etter maksdato
     * fjernes. Uten maksdato returneres periodene uendret.
     */
    public static Set<DatoIntervallEntitet> finnPerioderKappetMotMaksdato(UngdomsprogramPeriodeGrunnlag grunnlag) {
        var perioder = hentPerioder(grunnlag).stream().map(UngdomsprogramPeriode::getPeriode);
        var maksdato = grunnlag.getPeriodeMaksDato();
        if (maksdato.isEmpty()) {
            return perioder.collect(Collectors.toSet());
        }
        return perioder
            .filter(p -> !erEtterMaksdato(p.getFomDato(), maksdato.get()))
            .map(p -> DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), finnSisteDagMedYtelse(p.getTomDato(), maksdato.get())))
            .collect(Collectors.toSet());
    }

    /**
     * For datoer som ikke kommer fra samme grunnlag som maksdato.
     *
     * @return true dersom datoen ligger etter maksdato, dvs. utenfor perioden det kan gis ytelse for.
     */
    public static boolean erEtterMaksdato(LocalDate dato, LocalDate maksdato) {
        return dato.isAfter(maksdato);
    }

    private static LocalDate finnSisteDagMedYtelse(LocalDate programperiodeTom, LocalDate maksdato) {
        return programperiodeTom.isBefore(maksdato) ? programperiodeTom : maksdato;
    }

    private static LocalDate hentMaksdato(UngdomsprogramPeriodeGrunnlag grunnlag) {
        return grunnlag.getPeriodeMaksDato()
            .orElseThrow(() -> new IllegalStateException("Forventer at periodeMaksDato er satt på ungdomsprogramperiodegrunnlaget"));
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
