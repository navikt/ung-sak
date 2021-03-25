package no.nav.k9.sak.domene.arbeidsforhold.impl;

import static no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMapperUtledInntektsmeldingerTest.GenererImArbeidsforhold.inntektsmeldinger;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsforholdIdDto;
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class ArbeidsforholdMapperUtledInntektsmeldingerTest {

    private GenererImArbeidsforhold generator = new GenererImArbeidsforhold();

    @Test
    void uten_arbeidsforholdinformasjon() throws Exception {
        var mapper = new ArbeidsforholdMapper(null);
        mapper.utledArbeidsforholdFraInntektsmeldinger(new TreeSet<>());
        assertThat(mapper.getArbeidsforhold()).isEmpty();
    }

    @Test
    void håndter_im_med_og_uten_arbeidsforholdref_en_arbeidsgiver() throws Exception {
        var mapper = new ArbeidsforholdMapper(null);
        var ims = inntektsmeldinger(generator.inntektsmeldingMed().build(), generator.inntektsmeldingUten().build());
        mapper.utledArbeidsforholdFraInntektsmeldinger(ims);

        assertThat(mapper.getArbeidsforhold()).hasSize(2);
        List<ArbeidsforholdIdDto> arbeidsforhold = mapper.getArbeidsforhold().stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getArbeidsforhold).collect(Collectors.toList());
        assertThat(arbeidsforhold).hasSize(2);
    }

    @Test
    void håndter_im_med_og_uten_arbeidsforholdref_en_arbeidsgiver_er_uavhengig_av_rekkefølge() throws Exception {
        var arbeidsforholdSpes = generator.inntektsmeldingMed().build();

        var mapper1 = new ArbeidsforholdMapper(null);
        var ims1 = inntektsmeldinger(arbeidsforholdSpes, generator.inntektsmeldingUten().build());
        mapper1.utledArbeidsforholdFraInntektsmeldinger(ims1);
        List<ArbeidsforholdIdDto> arbeidsforhold1 = mapper1.getArbeidsforhold().stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getArbeidsforhold).collect(Collectors.toList());
        assertThat(arbeidsforhold1).hasSize(2);

        var mapper2 = new ArbeidsforholdMapper(null);
        var ims2 = inntektsmeldinger(generator.inntektsmeldingUten().build(), arbeidsforholdSpes);
        mapper2.utledArbeidsforholdFraInntektsmeldinger(ims2);
        List<ArbeidsforholdIdDto> arbeidsforhold2 = mapper2.getArbeidsforhold().stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getArbeidsforhold).collect(Collectors.toList());
        assertThat(arbeidsforhold2).hasSize(2);

        assertThat(arbeidsforhold1).isEqualTo(arbeidsforhold2);

    }

    @Test
    void håndter_im_med_og_uten_arbeidsforholdref_to_arbeidsgiver() throws Exception {
        var mapper1 = new ArbeidsforholdMapper(null);
        var ims1 = inntektsmeldinger(generator.inntektsmeldingUten(Arbeidsgiver.virksomhet("01")).build());
        mapper1.utledArbeidsforholdFraInntektsmeldinger(ims1);
        List<Arbeidsgiver> arbeidsgiver1 = mapper1.getArbeidsforhold().stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getArbeidsgiver).collect(Collectors.toList());
        assertThat(arbeidsgiver1).hasSize(1).allMatch(a -> a.getArbeidsgiverOrgnr().equals("01"));

        var mapper2 = new ArbeidsforholdMapper(null);
        var ims2 = inntektsmeldinger(generator.inntektsmeldingUten(Arbeidsgiver.virksomhet("02")).build());
        mapper2.utledArbeidsforholdFraInntektsmeldinger(ims2);
        List<Arbeidsgiver> arbeidsgiver2 = mapper2.getArbeidsforhold().stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getArbeidsgiver).collect(Collectors.toList());
        assertThat(arbeidsgiver2).hasSize(1).allMatch(a -> a.getArbeidsgiverOrgnr().equals("02"));

        assertThat(arbeidsgiver1).doesNotContainAnyElementsOf(arbeidsgiver2);

    }

    @Test
    void håndter_im_med_og_med_arbeidsforholdref_en_arbeidsgiver() throws Exception {
        var mapper = new ArbeidsforholdMapper(null);
        var ims = inntektsmeldinger(generator.inntektsmeldingMed().build(), generator.inntektsmeldingMed().build());
        mapper.utledArbeidsforholdFraInntektsmeldinger(ims);

        assertThat(mapper.getArbeidsforhold()).hasSize(2);
        List<ArbeidsforholdIdDto> arbeidsforhold = mapper.getArbeidsforhold().stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getArbeidsforhold).collect(Collectors.toList());
        assertThat(arbeidsforhold).hasSize(2);

        verifiserKunKilder(mapper.getArbeidsforhold(), ArbeidsforholdKilde.INNTEKTSMELDING);

    }

    private static void verifiserKunKilder(Set<InntektArbeidYtelseArbeidsforholdV2Dto> dtoer, ArbeidsforholdKilde expectedKilder) {
        var kilder = dtoer.stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getKilde).flatMap(v -> v.stream()).collect(Collectors.toSet());
        assertThat(kilder).containsOnly(expectedKilder);
    }

    static class GenererImArbeidsforhold {

        private static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet("80");

        private final AtomicInteger counter = new AtomicInteger(0);
        private final AtomicInteger kanalref = new AtomicInteger(0);

        private Arbeidsgiver virksomhet;

        GenererImArbeidsforhold() {
            this(VIRKSOMHET);
        }

        GenererImArbeidsforhold(Arbeidsgiver virksomhet) {
            this.virksomhet = virksomhet;
        }

        InntektsmeldingBuilder inntektsmeldingMed() {
            return inntektsmeldingMed(virksomhet);
        }

        InntektsmeldingBuilder inntektsmeldingMed(Arbeidsgiver arbeidsgiver) {
            var eksternRef = EksternArbeidsforholdRef.ref("im-ref-" + counter.incrementAndGet());
            var internRef = InternArbeidsforholdRef.namedRef(eksternRef.getReferanse());
            return inntektsmelding(arbeidsgiver, eksternRef, internRef);
        }

        InntektsmeldingBuilder inntektsmelding(EksternArbeidsforholdRef eksternRef, InternArbeidsforholdRef internRef) {
            return inntektsmeldingUten(virksomhet)
                .medArbeidsforholdId(eksternRef)
                .medArbeidsforholdId(internRef);
        }

        InntektsmeldingBuilder inntektsmelding(Arbeidsgiver arbeidsgiver, EksternArbeidsforholdRef eksternRef, InternArbeidsforholdRef internRef) {
            return inntektsmeldingUten(arbeidsgiver)
                .medArbeidsforholdId(eksternRef)
                .medArbeidsforholdId(internRef);
        }

        InntektsmeldingBuilder inntektsmeldingUten() {
            return inntektsmeldingUten(virksomhet);
        }

        InntektsmeldingBuilder inntektsmeldingUten(Arbeidsgiver arbeidsgiver) {
            int next = kanalref.incrementAndGet();
            return InntektsmeldingBuilder.builder()
                .medKanalreferanse("AltinnPortal-" + next)
                .medJournalpostId(String.valueOf(10000000 + next))
                .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
                .medArbeidsgiver(arbeidsgiver);
        }

        static NavigableSet<Inntektsmelding> inntektsmeldinger(InntektsmeldingBuilder... inntektsmeldinger) {
            var ims = new TreeSet<Inntektsmelding>(Inntektsmelding.COMP_REKKEFØLGE);
            for (var im : inntektsmeldinger) {
                ims.add(im.build());
            }
            return ims;
        }

        static NavigableSet<Inntektsmelding> inntektsmeldinger(Inntektsmelding... inntektsmeldinger) {
            var ims = new TreeSet<Inntektsmelding>(Inntektsmelding.COMP_REKKEFØLGE);
            for (var im : inntektsmeldinger) {
                ims.add(im);
            }
            return ims;
        }

    }
}
