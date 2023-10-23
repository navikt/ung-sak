package no.nav.k9.sak.skjæringstidspunkt;

import java.time.LocalDate;
import java.time.MonthDay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.k9.sak.typer.Periode;

public class SkattegrunnlaginnhentingTjeneste {

    public static final int MAKS_ANTALL_FERDIGLIGNEDE_ÅR = 4;
    public static final int MIN_ANTALL_FERDIGLIGNEDE_ÅR = 3;

    public static final MonthDay FØRSTE_MULIGE_SKATTEOPPGJØRSDATO = MonthDay.of(5, 1);

    private static final Logger LOGGER = LoggerFactory.getLogger(SkattegrunnlaginnhentingTjeneste.class);

    public static Periode utledSkattegrunnlagOpplysningsperiode(LocalDate førsteSkjæringstidspunkt, LocalDate fagsakperiodeTom, LocalDate dagensDato) {
        int sisteÅr = finnSisteÅr(fagsakperiodeTom, dagensDato);
        int førsteÅr = finnFørsteÅr(førsteSkjæringstidspunkt, dagensDato);
        if (førsteÅr <= 2015) {
            LOGGER.warn("Kutter opplysningsperiode for skattegrunnlag. Opprinnelig startår var " + førsteÅr);
            førsteÅr = 2016;
            if (sisteÅr <= 2015) {
                // abakus støtter ikkje å skippe innhenting av sigruninntekt
                // defaulter til å innhente for 2016, sjølv om fagsaken avsluttes tidligere
                LOGGER.warn("Flytter siste dato for opplysningsperiode for skattegrunnlag. Opprinnelig sluttår var " + sisteÅr);
                sisteÅr = 2016;

            }
        }
        return new Periode(LocalDate.of(førsteÅr, 1, 1), LocalDate.of(sisteÅr, 12, 31));
    }

    private static int finnFørsteÅr(LocalDate førsteSkjæringstidspunkt, LocalDate dagensDato) {
        var stpÅr = førsteSkjæringstidspunkt.getYear();
        int sisteTilgjengeligeÅr = finnSisteTilgjengeligeÅr(dagensDato);
        if (sisteTilgjengeligeÅr >= stpÅr - 1) {
            return stpÅr - MIN_ANTALL_FERDIGLIGNEDE_ÅR;
        } else {
            return stpÅr - MAKS_ANTALL_FERDIGLIGNEDE_ÅR;
        }
    }

    private static int finnSisteÅr(LocalDate fagsakperiodeTom, LocalDate dagensDato) {
        var åretFørFagsakTom = fagsakperiodeTom.getYear() - 1;
        int sisteTilgjengeligeÅr = finnSisteTilgjengeligeÅr(dagensDato);
        return Math.min(åretFørFagsakTom, sisteTilgjengeligeÅr);
    }

    private static int finnSisteTilgjengeligeÅr(LocalDate dagensDato) {
        var fjoråret = dagensDato.getYear() - 1;
        var sisteTilgjengeligeÅr = erSkatteoppgjørForÅretFørTilgjengelig(dagensDato) ? fjoråret : fjoråret - 1;
        return sisteTilgjengeligeÅr;
    }

    private static boolean erSkatteoppgjørForÅretFørTilgjengelig(LocalDate dagensDato) {
        return !MonthDay.of(dagensDato.getMonthValue(), dagensDato.getDayOfMonth()).isBefore(FØRSTE_MULIGE_SKATTEOPPGJØRSDATO);
    }

}
