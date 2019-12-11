package no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.Periode;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.HentBekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.vedtak.konfig.Tid;

public class FinnFørsteDagEtterBekreftetPermisjon {
    private FinnFørsteDagEtterBekreftetPermisjon() {
        // skjul public constructor
    }

    /**
     * @return Dersom det finnes en bekreftet permisjon som gjelder på skjæringstidspunkt for beregning, returner første dag etter den bekreftede permisjonen.
     * Ellers: Returner første dag i ansettelsesperioden til arbeidsforholdet. Dette kan være enten før eller etter første uttaksdag.
     */
  public static Optional<LocalDate> finn(InntektArbeidYtelseGrunnlag iayGrunnlag, Yrkesaktivitet ya, Periode ansettelsesPeriode, LocalDate skjæringstidspunktBeregning) {
        Optional<BekreftetPermisjon> permisjonForYrkesaktivitet = HentBekreftetPermisjon.hent(iayGrunnlag, ya);
        Optional<BekreftetPermisjon> bekreftetPermisjonOpt = permisjonForYrkesaktivitet
            .filter(perm -> perm.getStatus().equals(BekreftetPermisjonStatus.BRUK_PERMISJON))
            .filter(perm -> perm.getPeriode().inkluderer(skjæringstidspunktBeregning));
        if (bekreftetPermisjonOpt.isEmpty()) {
            return Optional.of(ansettelsesPeriode.getFom());
        }
        BekreftetPermisjon bekreftetPermisjon = bekreftetPermisjonOpt.get();
        LocalDate sisteDagMedPermisjon = bekreftetPermisjon.getPeriode().getTomDato();
        if (sisteDagMedPermisjon.equals(Tid.TIDENES_ENDE)) {
            return Optional.empty();
        }
        LocalDate dagenEtterBekreftetPermisjon = sisteDagMedPermisjon.plusDays(1);
        return Optional.of(dagenEtterBekreftetPermisjon);
    }
}
