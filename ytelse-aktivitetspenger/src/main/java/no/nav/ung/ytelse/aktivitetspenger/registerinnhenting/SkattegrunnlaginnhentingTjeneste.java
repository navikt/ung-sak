package no.nav.ung.ytelse.aktivitetspenger.registerinnhenting;

import no.nav.ung.sak.typer.Periode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.MonthDay;

public class SkattegrunnlaginnhentingTjeneste {

    public static final int ANTALL_FERDIGLIGNEDE_ÅR = 3;

    public static final MonthDay FØRSTE_MULIGE_SKATTEOPPGJØRSDATO = MonthDay.of(5, 1);

    private static final Logger LOGGER = LoggerFactory.getLogger(SkattegrunnlaginnhentingTjeneste.class);

    // TODO: Denne må skrives om til å kunne innhente for fleire skjæringstidspunkter når vi kjem dit (nye perioder)
    public static Periode utledSkattegrunnlagOpplysningsperiode(LocalDate førsteSkjæringstidspunkt, LocalDate dagensDato) {
        int sisteÅr = finnSisteÅr(førsteSkjæringstidspunkt, dagensDato);
        int førsteÅr = finnFørsteÅr(førsteSkjæringstidspunkt);
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

    private static int finnFørsteÅr(LocalDate førsteSkjæringstidspunkt) {
        var stpÅr = førsteSkjæringstidspunkt.getYear();
        return stpÅr - ANTALL_FERDIGLIGNEDE_ÅR;
    }

    private static int finnSisteÅr(LocalDate førsteSkjæringstidspunkt, LocalDate dagensDato) {
        var åretFørFørsteSkjæringstidspunkt = førsteSkjæringstidspunkt.getYear() - 1;
        int sisteTilgjengeligeÅr = finnSisteTilgjengeligeÅr(dagensDato);
        return Math.min(åretFørFørsteSkjæringstidspunkt, sisteTilgjengeligeÅr);
    }

    private static int finnSisteTilgjengeligeÅr(LocalDate dagensDato) {
        var fjoråret = dagensDato.getYear() - 1;
        var sisteTilgjengeligeÅr = kanSkatteoppgjørForÅretFørVæreTilgjengelig(dagensDato) ? fjoråret : fjoråret - 1;
        return sisteTilgjengeligeÅr;
    }

    private static boolean kanSkatteoppgjørForÅretFørVæreTilgjengelig(LocalDate dagensDato) {
        return !MonthDay.of(dagensDato.getMonthValue(), dagensDato.getDayOfMonth()).isBefore(FØRSTE_MULIGE_SKATTEOPPGJØRSDATO);
    }

}
