package no.nav.k9.sak.domene.arbeidsforhold.impl;

import static no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMapperUtledInntektsmeldingerTest.GenererImArbeidsforhold.inntektsmeldinger;
import static no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMapperUtledYrkesaktivitetTest.GenererYrkArbeidsforhold.yrkesaktiviteter;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMapperUtledInntektsmeldingerTest.GenererImArbeidsforhold;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMapperUtledYrkesaktivitetTest.GenererYrkArbeidsforhold;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdOverstyring;
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class ArbeidsforholdMapperKomboUtlederTest {

    private static final ArbeidsforholdKilde INNTEKTSMELDING = ArbeidsforholdKilde.INNTEKTSMELDING;
    private static final ArbeidsforholdKilde AAREG = ArbeidsforholdKilde.AAREGISTERET;

    private static final Arbeidsgiver VIRKSOMHET_0 = Arbeidsgiver.virksomhet("90");
    private static final Arbeidsgiver VIRKSOMHET_1 = Arbeidsgiver.virksomhet("91");
    private final AtomicInteger counter = new AtomicInteger(0);

    private ArbeidsforholdAdministrasjonTjeneste tjeneste = new ArbeidsforholdAdministrasjonTjeneste();

    @Test
    public void legg_til_kilder_på_matchende_im_og_yrk() throws Exception {
        var eksternRef = nesteEksternRef();
        var internRef = nesteInternRef();

        var yrkGenerator = new GenererYrkArbeidsforhold(VIRKSOMHET_0);
        var yrk1 = yrkGenerator.yrkesaktivitetMed(eksternRef, internRef);
        var yrk2 = yrkGenerator.yrkesaktivitetUten();
        var yrkUtenfor = yrkGenerator.yrkesaktivitetMed(VIRKSOMHET_1);

        var imGenerator = new GenererImArbeidsforhold(VIRKSOMHET_0);
        var im1 = imGenerator.inntektsmelding(eksternRef, internRef);
        var im2 = imGenerator.inntektsmeldingUten();
        var imUtenfor = imGenerator.inntektsmeldingMed(VIRKSOMHET_1);

        var mapper = new ArbeidsforholdMapper(null);

        tjeneste.mapArbeidsforhold(mapper,
            yrkesaktiviteter(yrk1, yrk2, yrkUtenfor),
            overstyringer(),
            inntektsmeldinger(im1, im2, imUtenfor));

        // matchende
        verifiserKilder(mapper.getArbeidsforhold(), yrk1.getArbeidsforholdRef(), AAREG, INNTEKTSMELDING);
        verifiserKilder(mapper.getArbeidsforhold(), yrk2.getArbeidsforholdRef(), AAREG, INNTEKTSMELDING);

        // disse er samme som over
        verifiserKilder(mapper.getArbeidsforhold(), im1.getInternArbeidsforholdRef().orElseThrow(), AAREG, INNTEKTSMELDING);
        verifiserKilder(mapper.getArbeidsforhold(), im2.getInternArbeidsforholdRef().orElseThrow(), AAREG, INNTEKTSMELDING);

        // yrk utenfor
        verifiserKilder(mapper.getArbeidsforhold(), yrkUtenfor.getArbeidsforholdRef(), AAREG);

        // im utenfor
        verifiserKilder(mapper.getArbeidsforhold(), imUtenfor.getInternArbeidsforholdRef().orElseThrow(), INNTEKTSMELDING);

    }

    private void verifiserKilder(Set<InntektArbeidYtelseArbeidsforholdV2Dto> arbeidsforhold,
                                 InternArbeidsforholdRef internRef,
                                 ArbeidsforholdKilde... kilder) {
        assertThat(arbeidsforhold).isNotEmpty();
        var dtoer = arbeidsforhold.stream()
            .filter(dto -> Objects.equals(internRef.getUUIDReferanse(), dto.getArbeidsforhold().getInternArbeidsforholdId()))
            .collect(Collectors.toList());

        assertThat(dtoer).as("Fant ikke arbeidsforhold: " + internRef).hasSize(1);
        assertThat(dtoer.stream()).allSatisfy(dto -> assertThat(dto.getKilde()).containsOnly(kilder));

    }

    private EksternArbeidsforholdRef nesteEksternRef() {
        return EksternArbeidsforholdRef.ref("ref-" + counter.incrementAndGet());
    }

    private InternArbeidsforholdRef nesteInternRef() {
        return InternArbeidsforholdRef.nyRef();
    }

    private static List<ArbeidsforholdOverstyring> overstyringer(ArbeidsforholdOverstyring... ovs) {
        return List.of(ovs);
    }

}

