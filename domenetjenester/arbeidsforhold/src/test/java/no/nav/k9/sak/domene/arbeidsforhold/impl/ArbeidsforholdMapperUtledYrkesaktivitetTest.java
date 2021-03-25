package no.nav.k9.sak.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.k9.sak.domene.iay.modell.AktivitetsAvtaleBuilder;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjon;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.domene.iay.modell.Permisjon;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class ArbeidsforholdMapperUtledYrkesaktivitetTest {

    @Test
    void uten_arbeidsforholdinformasjon() throws Exception {
        var mapper = new ArbeidsforholdMapper(null);
        mapper.utledArbeidsforholdFraYrkesaktiviteter(List.of());
        assertThat(mapper.getArbeidsforhold()).isEmpty();
    }

    @Test
    void håndter_yrkesaktiviteter_arbeidsforhold_med_og_uten() throws Exception {
        var generator = new GenererYrkArbeidsforhold();
        var med = generator.yrkesaktivitetMed();
        var uten = generator.yrkesaktivitetUten();

        var mapper = new ArbeidsforholdMapper(generator.getArbeidsforholdInformasjon());
        mapper.utledArbeidsforholdFraYrkesaktiviteter(List.of(med, uten));
        assertThat(mapper.getArbeidsforhold()).hasSize(2);
        verifiserKunKilder(mapper.getArbeidsforhold(), ArbeidsforholdKilde.AAREGISTERET);
    }

    @Test
    void håndter_yrkesaktiviteter_arbeidsforhold_med_og_med() throws Exception {
        var generator = new GenererYrkArbeidsforhold();
        var med1 = generator.yrkesaktivitetMed();
        var med2 = generator.yrkesaktivitetMed();

        var mapper = new ArbeidsforholdMapper(generator.getArbeidsforholdInformasjon());
        mapper.utledArbeidsforholdFraYrkesaktiviteter(List.of(med1, med2));
        assertThat(mapper.getArbeidsforhold()).hasSize(2);
        verifiserKunKilder(mapper.getArbeidsforhold(), ArbeidsforholdKilde.AAREGISTERET);

    }

    private static void verifiserKunKilder(Set<InntektArbeidYtelseArbeidsforholdV2Dto> dtoer, ArbeidsforholdKilde expectedKilder) {
        var kilder = dtoer.stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getKilde).flatMap(v -> v.stream()).collect(Collectors.toSet());
        assertThat(kilder).containsOnly(expectedKilder);
    }

    static class GenererYrkArbeidsforhold {
        private static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet("70");
        private final AtomicInteger counter = new AtomicInteger(0);

        private ArbeidType arbeidType = ArbeidType.ORDINÆRT_ARBEIDSFORHOLD;
        private Arbeidsgiver virksomhet;
        private ArbeidsforholdInformasjonBuilder arbeidsforholdBuilder = ArbeidsforholdInformasjonBuilder.builder(Optional.empty());

        LocalDate fom = LocalDate.now().minusDays(20);
        LocalDate tom = fom.plusDays(100);

        GenererYrkArbeidsforhold() {
            this(VIRKSOMHET);
        }

        GenererYrkArbeidsforhold(Arbeidsgiver virksomhet) {
            this.virksomhet = virksomhet;
        }

        ArbeidsforholdInformasjon getArbeidsforholdInformasjon() {
            return arbeidsforholdBuilder.build();
        }

        Yrkesaktivitet yrkesaktivitetMed() {
            var eksternRef = EksternArbeidsforholdRef.ref("yrk-ref-" + counter.incrementAndGet());
            var internRef = InternArbeidsforholdRef.namedRef(eksternRef.getReferanse());
            return yrkesaktivitetMed(eksternRef, internRef);
        }

        Yrkesaktivitet yrkesaktivitetMed(Arbeidsgiver arbeidsgiver) {
            var eksternRef = EksternArbeidsforholdRef.ref("yrk-ref-" + counter.incrementAndGet());
            var internRef = InternArbeidsforholdRef.namedRef(eksternRef.getReferanse());
            return yrkesaktivitetMed(arbeidsgiver, eksternRef, internRef);
        }

        Yrkesaktivitet yrkesaktivitetMed(EksternArbeidsforholdRef eksternArbeidsforhold, InternArbeidsforholdRef internArbeidsforhold) {
            return yrkesaktivitetMed(virksomhet, eksternArbeidsforhold, internArbeidsforhold);
        }

        Yrkesaktivitet yrkesaktivitetMed(Arbeidsgiver arbeidsgiver, EksternArbeidsforholdRef eksternArbeidsforhold, InternArbeidsforholdRef internArbeidsforhold) {
            arbeidsforholdBuilder.leggTil(arbeidsgiver, internArbeidsforhold, eksternArbeidsforhold);
            AktivitetsAvtaleBuilder ansettelsesperiode = YrkesaktivitetBuilder.nyAktivitetsAvtaleBuilder()
                .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
            return byggYrkesAktivitet(null, ansettelsesperiode, arbeidsgiver.getArbeidsgiverOrgnr(), arbeidType, internArbeidsforhold);
        }

        Yrkesaktivitet yrkesaktivitetUten() {
            var internRef = InternArbeidsforholdRef.nullRef();
            return yrkesaktivitetMed(virksomhet, null, internRef);
        }

        private Yrkesaktivitet byggYrkesAktivitet(Permisjon permisjon, AktivitetsAvtaleBuilder aktivitetsAvtale, String orgNr, ArbeidType arbeidType, InternArbeidsforholdRef internArbeidsforhold) {
            return YrkesaktivitetBuilder.oppdatere(Optional.empty())
                .medArbeidType(arbeidType)
                .medArbeidsforholdId(internArbeidsforhold)
                .medArbeidsgiver(Arbeidsgiver.virksomhet(orgNr))
                .medArbeidType(arbeidType)
                .leggTilAktivitetsAvtale(aktivitetsAvtale)
                .leggTilPermisjon(permisjon)
                .build();
        }

        static List<Yrkesaktivitet> yrkesaktiviteter(Yrkesaktivitet... yrkesaktiviter) {
            return List.of(yrkesaktiviter);
        }
    }

}
