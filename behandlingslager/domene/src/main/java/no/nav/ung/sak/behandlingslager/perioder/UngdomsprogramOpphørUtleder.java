package no.nav.ung.sak.behandlingslager.perioder;

import no.nav.k9.felles.konfigurasjon.konfig.Tid;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

/**
 * Svarer på om programperioden i et periodegrunnlag er lukket eller åpen. Kallestedet henter selv grunnlaget det
 * er interessert i - for behandlingen selv eller for originalbehandlingen.
 */
public final class UngdomsprogramOpphørUtleder {

    private UngdomsprogramOpphørUtleder() {
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
