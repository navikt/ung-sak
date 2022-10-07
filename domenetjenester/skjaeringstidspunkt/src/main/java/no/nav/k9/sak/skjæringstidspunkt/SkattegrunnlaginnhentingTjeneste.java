package no.nav.k9.sak.skjæringstidspunkt;

import java.time.LocalDate;
import java.time.MonthDay;

import no.nav.k9.sak.typer.Periode;

public class SkattegrunnlaginnhentingTjeneste {

    public static final int ANTALL_FERDIGLIGNEDE_ÅR = 4;
    public static final MonthDay FØRSTE_MULIGE_SKATTEOPPGJØRSDATO = MonthDay.of(5, 1);

    public static Periode utledSkattegrunnlagOpplysningsperiode(LocalDate førsteSkjæringstidspunkt, LocalDate fagsakperiodeTom) {
        int sisteÅr = finnSisteÅr(fagsakperiodeTom);
        int førsteÅr = finnFørsteÅr(førsteSkjæringstidspunkt);
        return new Periode(LocalDate.of(førsteÅr, 1, 1), LocalDate.of(sisteÅr, 12, 31));
    }

    private static int finnFørsteÅr(LocalDate førsteSkjæringstidspunkt) {
        var åretFørFørsteStp = førsteSkjæringstidspunkt.getYear() - 1;
        return åretFørFørsteStp - ANTALL_FERDIGLIGNEDE_ÅR;
    }

    private static int finnSisteÅr(LocalDate fagsakperiodeTom) {
        var åretFørFagsakTom = fagsakperiodeTom.getYear() - 1;
        var fjoråret = LocalDate.now().getYear() - 1;
        var sisteTilgjengeligeÅr = kanFjoråretVæreTilgjengelig() ? fjoråret : fjoråret - 1;
        return Math.min(åretFørFagsakTom, sisteTilgjengeligeÅr);
    }

    private static boolean kanFjoråretVæreTilgjengelig() {
        return MonthDay.now().isAfter(FØRSTE_MULIGE_SKATTEOPPGJØRSDATO);
    }

}
