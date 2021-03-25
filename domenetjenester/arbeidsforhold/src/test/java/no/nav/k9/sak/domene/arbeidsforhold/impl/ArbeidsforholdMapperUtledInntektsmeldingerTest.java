package no.nav.k9.sak.domene.arbeidsforhold.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.domene.iay.modell.Inntektsmelding;
import no.nav.k9.sak.domene.iay.modell.InntektsmeldingBuilder;
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

        var ims = setOf(nyttArbeidsforholdSpes(), nyttArbeidsforholdGen());
        mapper.utledArbeidsforholdFraInntektsmeldinger(ims);

        assertThat(mapper.getArbeidsforhold()).hasSize(2);

    }

    @Test
    void håndter_im_med_og_med_arbeidsforholdref_en_arbeidsgiver() throws Exception {
        var mapper = new ArbeidsforholdMapper(null);

        var ims = setOf(nyttArbeidsforholdSpes(), nyttArbeidsforholdSpes());

        mapper.utledArbeidsforholdFraInntektsmeldinger(ims);

        assertThat(mapper.getArbeidsforhold()).hasSize(2);

    }

    private static InntektsmeldingBuilder nyttArbeidsforholdSpes() {
        return nyttArbeidsforholdGen()
            .medArbeidsforholdId(EksternArbeidsforholdRef.ref("ref-" + COUNTER.incrementAndGet()))
            .medArbeidsforholdId(InternArbeidsforholdRef.nyRef());
    }

    private static InntektsmeldingBuilder nyttArbeidsforholdGen() {
        int next = KANALREF.incrementAndGet();
        return InntektsmeldingBuilder.builder()
            .medKanalreferanse("AltinnPortal-" + next)
            .medJournalpostId(String.valueOf(10000000 + next))
            .medArbeidsforholdId(InternArbeidsforholdRef.nullRef())
            .medArbeidsgiver(VIRKSOMHET);
    }

    public static NavigableSet<Inntektsmelding> setOf(InntektsmeldingBuilder... builders) {
        var ims = new TreeSet<Inntektsmelding>(Inntektsmelding.COMP_REKKEFØLGE);
        for (var im : builders) {
            ims.add(im.build());
        }
        return ims;
    }
}
