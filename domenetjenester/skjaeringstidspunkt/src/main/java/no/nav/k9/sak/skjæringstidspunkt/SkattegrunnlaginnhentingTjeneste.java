package no.nav.k9.sak.skjæringstidspunkt;

import java.time.LocalDate;
import java.time.MonthDay;

import no.nav.k9.sak.typer.Periode;

public class SkattegrunnlaginnhentingTjeneste {

    public static final int MAKS_ANTALL_FERDIGLIGNEDE_ÅR = 4;
    public static final int MIN_ANTALL_FERDIGLIGNEDE_ÅR = 3;

    public static final MonthDay FØRSTE_MULIGE_SKATTEOPPGJØRSDATO = MonthDay.of(5, 1);

    public static Periode utledSkattegrunnlagOpplysningsperiode(LocalDate førsteSkjæringstidspunkt, LocalDate fagsakperiodeTom) {
        int sisteÅr = finnSisteÅr(fagsakperiodeTom);
        int førsteÅr = finnFørsteÅr(førsteSkjæringstidspunkt);
        if (førsteÅr <= 2015) {
            throw new IllegalStateException("Første år må være etter 2015");
        }
        return new Periode(LocalDate.of(førsteÅr, 1, 1), LocalDate.of(sisteÅr, 12, 31));
    }

    private static int finnFørsteÅr(LocalDate førsteSkjæringstidspunkt) {
        var stpÅr = førsteSkjæringstidspunkt.getYear();
        int sisteTilgjengeligeÅr = finnSisteTilgjengeligeÅr();
        if (sisteTilgjengeligeÅr == stpÅr - 1) {
            return stpÅr - MIN_ANTALL_FERDIGLIGNEDE_ÅR;
        } else {
            return stpÅr - MAKS_ANTALL_FERDIGLIGNEDE_ÅR;
        }
    }

    private static int finnSisteÅr(LocalDate fagsakperiodeTom) {
        var åretFørFagsakTom = fagsakperiodeTom.getYear() - 1;
        int sisteTilgjengeligeÅr = finnSisteTilgjengeligeÅr();
        return Math.min(åretFørFagsakTom, sisteTilgjengeligeÅr);
    }

    private static int finnSisteTilgjengeligeÅr() {
        var fjoråret = LocalDate.now().getYear() - 1;
        var sisteTilgjengeligeÅr = erSkatteoppgjørForÅretFørTilgjengelig() ? fjoråret : fjoråret - 1;
        return sisteTilgjengeligeÅr;
    }

    private static boolean erSkatteoppgjørForÅretFørTilgjengelig() {
        return MonthDay.now().isAfter(FØRSTE_MULIGE_SKATTEOPPGJØRSDATO);
    }

}
