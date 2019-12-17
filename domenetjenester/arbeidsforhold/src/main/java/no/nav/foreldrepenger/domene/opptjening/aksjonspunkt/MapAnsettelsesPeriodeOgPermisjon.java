package no.nav.foreldrepenger.domene.opptjening.aksjonspunkt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import no.nav.foreldrepenger.domene.arbeidsforhold.impl.HentBekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtale;
import no.nav.foreldrepenger.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.foreldrepenger.domene.iay.modell.BekreftetPermisjon;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.BekreftetPermisjonStatus;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;

class MapAnsettelsesPeriodeOgPermisjon {

    private MapAnsettelsesPeriodeOgPermisjon() {
        // skjul public constructor
    }

    static Collection<AktivitetsAvtale> beregn(InntektArbeidYtelseGrunnlag grunnlag, Yrkesaktivitet yrkesaktivitet) {
        Optional<BekreftetPermisjon> bekreftetPermisjonOpt = HentBekreftetPermisjon.hent(grunnlag, yrkesaktivitet);
        var filter = new YrkesaktivitetFilter(grunnlag.getArbeidsforholdInformasjon(), yrkesaktivitet);
        List<AktivitetsAvtale> ansettelsesPerioder = filter.getAnsettelsesPerioder(yrkesaktivitet);
        if (!bekreftetPermisjonOpt.isPresent()) {
            return ansettelsesPerioder;
        }
        BekreftetPermisjon bekreftetPermisjon = bekreftetPermisjonOpt.get();
        if (bekreftetPermisjon.getStatus().equals(BekreftetPermisjonStatus.BRUK_PERMISJON)) {
            return utledPerioder(filter, yrkesaktivitet, bekreftetPermisjon);
        }
        return ansettelsesPerioder;
    }

    private static Collection<AktivitetsAvtale> utledPerioder(YrkesaktivitetFilter filter, Yrkesaktivitet yrkesaktivitet, BekreftetPermisjon bekreftetPermisjon) {
        DatoIntervallEntitet permisjonPeriode = bekreftetPermisjon.getPeriode();
        List<AktivitetsAvtale> justerteAnsettesesPerioder = new ArrayList<>();
        filter.getAnsettelsesPerioder(yrkesaktivitet).forEach(ap -> {
            if (ap.getPeriode().overlapper(permisjonPeriode)) {
                if (ap.getPeriode().getFomDato().isBefore(permisjonPeriode.getFomDato())) {
                    // legg til periode før permisjonen
                    DatoIntervallEntitet nyPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(
                        ap.getPeriode().getFomDato(),
                        permisjonPeriode.getFomDato().minusDays(1)
                    );
                    AktivitetsAvtale nyAp = AktivitetsAvtaleBuilder.ny()
                        .medBeskrivelse(ap.getBeskrivelse())
                        .medPeriode(nyPeriode)
                        .build();
                    justerteAnsettesesPerioder.add(nyAp);
                }
                if (ap.getPeriode().getTomDato().isAfter(permisjonPeriode.getTomDato())) {
                    // legg til periode etter permisjonen
                    DatoIntervallEntitet nyPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(
                        permisjonPeriode.getTomDato().plusDays(1),
                        ap.getPeriode().getTomDato()
                    );
                    AktivitetsAvtale nyAp = AktivitetsAvtaleBuilder.ny()
                        .medBeskrivelse(ap.getBeskrivelse())
                        .medPeriode(nyPeriode)
                        .build();
                    justerteAnsettesesPerioder.add(nyAp);
                }
            } else {
                justerteAnsettesesPerioder.add(ap);
            }
        });
        return justerteAnsettesesPerioder;
    }

}
