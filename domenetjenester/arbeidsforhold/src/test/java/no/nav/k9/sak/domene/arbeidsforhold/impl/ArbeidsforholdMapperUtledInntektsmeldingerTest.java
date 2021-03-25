package no.nav.k9.sak.domene.arbeidsforhold.impl;

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

    private static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet("900300200");

    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private static final AtomicInteger KANALREF = new AtomicInteger(0);

    @Test
    void uten_arbeidsforholdinformasjon() throws Exception {
        var mapper = new ArbeidsforholdMapper(null);
        mapper.utledArbeidsforholdFraArbeidsforholdInformasjon(List.of());
        assertThat(mapper.getArbeidsforhold()).isEmpty();
    }

    @Test
    void håndter_im_med_og_uten_arbeidsforholdref_en_arbeidsgiver() throws Exception {
        var mapper = new ArbeidsforholdMapper(null);
        var ims = setOf(nyttArbeidsforholdSpes().build(), nyttArbeidsforholdGen().build());
        mapper.utledArbeidsforholdFraInntektsmeldinger(ims);

        assertThat(mapper.getArbeidsforhold()).hasSize(2);
        List<ArbeidsforholdIdDto> arbeidsforhold = mapper.getArbeidsforhold().stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getArbeidsforhold).collect(Collectors.toList());
        assertThat(arbeidsforhold).hasSize(2);
    }

    @Test
    void håndter_im_med_og_uten_arbeidsforholdref_en_arbeidsgiver_er_uavhengig_av_rekkefølge() throws Exception {
        var arbeidsforholdSpes = nyttArbeidsforholdSpes().build();

        var mapper1 = new ArbeidsforholdMapper(null);
        var ims1 = setOf(arbeidsforholdSpes, nyttArbeidsforholdGen().build());
        mapper1.utledArbeidsforholdFraInntektsmeldinger(ims1);
        List<ArbeidsforholdIdDto> arbeidsforhold1 = mapper1.getArbeidsforhold().stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getArbeidsforhold).collect(Collectors.toList());
        assertThat(arbeidsforhold1).hasSize(2);

        var mapper2 = new ArbeidsforholdMapper(null);
        var ims2 = setOf(nyttArbeidsforholdGen().build(), arbeidsforholdSpes);
        mapper2.utledArbeidsforholdFraInntektsmeldinger(ims2);
        List<ArbeidsforholdIdDto> arbeidsforhold2 = mapper2.getArbeidsforhold().stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getArbeidsforhold).collect(Collectors.toList());
        assertThat(arbeidsforhold2).hasSize(2);

        assertThat(arbeidsforhold1).isEqualTo(arbeidsforhold2);

    }

    @Test
    void håndter_im_med_og_uten_arbeidsforholdref_to_arbeidsgiver() throws Exception {
        var mapper1 = new ArbeidsforholdMapper(null);
        var ims1 = setOf(nyttArbeidsforholdGen(Arbeidsgiver.virksomhet("01")).build());
        mapper1.utledArbeidsforholdFraInntektsmeldinger(ims1);
        List<Arbeidsgiver> arbeidsgiver1 = mapper1.getArbeidsforhold().stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getArbeidsgiver).collect(Collectors.toList());
        assertThat(arbeidsgiver1).hasSize(1).allMatch(a -> a.getArbeidsgiverOrgnr().equals("01"));

        var mapper2 = new ArbeidsforholdMapper(null);
        var ims2 = setOf(nyttArbeidsforholdGen(Arbeidsgiver.virksomhet("02")).build());
        mapper2.utledArbeidsforholdFraInntektsmeldinger(ims2);
        List<Arbeidsgiver> arbeidsgiver2 = mapper2.getArbeidsforhold().stream().map(InntektArbeidYtelseArbeidsforholdV2Dto::getArbeidsgiver).collect(Collectors.toList());
        assertThat(arbeidsgiver2).hasSize(1).allMatch(a -> a.getArbeidsgiverOrgnr().equals("02"));

        assertThat(arbeidsgiver1).doesNotContainAnyElementsOf(arbeidsgiver2);

    }

    @Test
    void håndter_im_med_og_med_arbeidsforholdref_en_arbeidsgiver() throws Exception {
        var mapper = new ArbeidsforholdMapper(null);
        var ims = setOf(nyttArbeidsforholdSpes().build(), nyttArbeidsforholdSpes().build());
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

    private static InntektsmeldingBuilder nyttArbeidsforholdSpes() {
        return nyttArbeidsforholdSpes(VIRKSOMHET);
    }

    private static InntektsmeldingBuilder nyttArbeidsforholdSpes(Arbeidsgiver arbeidsgiver) {
        return nyttArbeidsforholdGen(arbeidsgiver)
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("ref-" + COUNTER.incrementAndGet()))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef());
    }

    private static InntektsmeldingBuilder nyttArbeidsforholdGen() {
        return nyttArbeidsforholdGen(VIRKSOMHET);
    }

    private static InntektsmeldingBuilder nyttArbeidsforholdGen(Arbeidsgiver arbeidsgiver) {
        int next = KANALREF.incrementAndGet();
        return InntektsmeldingBuilder.builder()
            .medKanalreferanse("AltinnPortal-" + next)
            .medJournalpostId(String.valueOf(10000000 + next))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsgiver(arbeidsgiver);
    }

    private static NavigableSet<Inntektsmelding> setOf(Inntektsmelding... inntektsmeldinger) {
        var ims = new TreeSet<Inntektsmelding>(Inntektsmelding.COMP_REKKEFØLGE);
        for (var im : inntektsmeldinger) {
            ims.add(im);
        }
        return ims;
    }
}
