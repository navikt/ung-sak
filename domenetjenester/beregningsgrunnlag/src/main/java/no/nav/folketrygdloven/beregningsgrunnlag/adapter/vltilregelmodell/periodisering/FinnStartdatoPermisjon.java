package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;

import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;

public class FinnStartdatoPermisjon {
    private FinnStartdatoPermisjon() {
        // skjul public constructor
    }

    /**
     * @param ya yrkesaktiviteten
     * @param førsteuttaksdag første ønskede dag med uttak av foreldrepenger
     * @param startdatoForArbeid       første dag i aktiviteten. Kan være før første uttaksdag,
*                        eller etter første uttaksdag dersom bruker starter i arbeidsforholdet
*                        eller er i permisjon (f.eks. PERMITTERT) ved første uttaksdag.
*                        Se {@link PermisjonsbeskrivelseType}
     * @param inntektsmeldinger inntektsmeldinger som er gyldige/aktive
     */
    public static LocalDate finnStartdatoPermisjon(Yrkesaktivitet ya, LocalDate førsteuttaksdag, LocalDate startdatoForArbeid, Collection<Inntektsmelding>inntektsmeldinger) {
        return startdatoForArbeid.isBefore(førsteuttaksdag) ? førsteuttaksdag : utledStartdato(ya, startdatoForArbeid, inntektsmeldinger);
    }

    private static LocalDate utledStartdato(Yrkesaktivitet ya, LocalDate startdatoForArbeid, Collection<Inntektsmelding>inntektsmeldinger) {
        Optional<Inntektsmelding> matchendeInntektsmelding = inntektsmeldinger.stream()
            .filter(im -> ya.gjelderFor(im.getArbeidsgiver(), im.getArbeidsforholdRef()))
            .findFirst();
        Optional<LocalDate> startDatoFraIM = matchendeInntektsmelding.flatMap(Inntektsmelding::getStartDatoPermisjon);
        return startDatoFraIM.filter(dato -> dato.isAfter(startdatoForArbeid)).orElse(startdatoForArbeid);
    }
}
