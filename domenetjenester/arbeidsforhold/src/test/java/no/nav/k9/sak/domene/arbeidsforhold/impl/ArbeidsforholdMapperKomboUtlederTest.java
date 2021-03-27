package no.nav.k9.sak.domene.arbeidsforhold.impl;

import static no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde.SAKSBEHANDLER;
import static no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMapperUtledFraArbeidsforholdInformasjonTest.GenererArbinfoArbeidsforhold.overstyringer;
import static no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMapperUtledInntektsmeldingerTest.GenererImArbeidsforhold.inntektsmeldinger;
import static no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMapperUtledYrkesaktivitetTest.GenererYrkArbeidsforhold.yrkesaktiviteter;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdHandlingType;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidsforholdKilde;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMapperUtledFraArbeidsforholdInformasjonTest.GenererArbinfoArbeidsforhold;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMapperUtledInntektsmeldingerTest.GenererImArbeidsforhold;
import no.nav.k9.sak.domene.arbeidsforhold.impl.ArbeidsforholdMapperUtledYrkesaktivitetTest.GenererYrkArbeidsforhold;
import no.nav.k9.sak.domene.iay.modell.ArbeidsforholdInformasjonBuilder;
import no.nav.k9.sak.kontrakt.arbeidsforhold.ArbeidsforholdAksjonspunktÅrsak;
import no.nav.k9.sak.kontrakt.arbeidsforhold.InntektArbeidYtelseArbeidsforholdV2Dto;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.k9.sak.typer.EksternArbeidsforholdRef;
import no.nav.k9.sak.typer.InternArbeidsforholdRef;

public class ArbeidsforholdMapperKomboUtlederTest {

    private static final ArbeidsforholdKilde INNTEKTSMELDING = ArbeidsforholdKilde.INNTEKTSMELDING;
    private static final ArbeidsforholdKilde AAREG = ArbeidsforholdKilde.AAREGISTERET;

    private final AtomicInteger counter = new AtomicInteger(0);

    private ArbeidsforholdAdministrasjonTjeneste tjeneste = new ArbeidsforholdAdministrasjonTjeneste();

    @Test
    public void sjekk_kilder_matcher_på_overlappende_og_ikke_overlappende_aareg_og_inntektsmelding_arbeidsforhold() throws Exception {
        Arbeidsgiver arbeidsgiver_01 = Arbeidsgiver.virksomhet("01");
        Arbeidsgiver arbeidsgiver_02 = Arbeidsgiver.virksomhet("02");
        var eksternRef = nesteEksternRef();
        var internRef = nesteInternRef();

        var yrkGenerator = new GenererYrkArbeidsforhold(arbeidsgiver_01);
        var yrk1 = yrkGenerator.yrkesaktivitetMed(eksternRef, internRef);
        var yrk2 = yrkGenerator.yrkesaktivitetUten();
        var yrkSomIkkeFårIm = yrkGenerator.yrkesaktivitetMed(arbeidsgiver_02);

        var imGenerator = new GenererImArbeidsforhold(arbeidsgiver_01);
        var im1 = imGenerator.inntektsmelding(eksternRef, internRef);
        var im2 = imGenerator.inntektsmeldingUten();
        var imForArbeidforholdIkkeIAareg = imGenerator.inntektsmeldingMed(arbeidsgiver_02);

        var mapper = new ArbeidsforholdMapper(null);

        tjeneste.mapArbeidsforhold(mapper,
            yrkesaktiviteter(yrk1, yrk2, yrkSomIkkeFårIm),
            overstyringer(),
            inntektsmeldinger(im1, im2, imForArbeidforholdIkkeIAareg));

        // matchende
        verifiserKilder(mapper.getArbeidsforhold(), arbeidsgiver_01, yrk1.getArbeidsforholdRef(), AAREG, INNTEKTSMELDING);
        verifiserKilder(mapper.getArbeidsforhold(), arbeidsgiver_01, yrk2.getArbeidsforholdRef(), AAREG, INNTEKTSMELDING);

        // disse er samme som over
        verifiserKilder(mapper.getArbeidsforhold(), arbeidsgiver_01, im1.getInternArbeidsforholdRef().orElseThrow(), AAREG, INNTEKTSMELDING);
        verifiserKilder(mapper.getArbeidsforhold(), arbeidsgiver_01, im2.getInternArbeidsforholdRef().orElseThrow(), AAREG, INNTEKTSMELDING);

        // yrk utenfor
        verifiserKilder(mapper.getArbeidsforhold(), arbeidsgiver_02, yrkSomIkkeFårIm.getArbeidsforholdRef(), AAREG);

        // im utenfor
        verifiserKilder(mapper.getArbeidsforhold(), arbeidsgiver_02, imForArbeidforholdIkkeIAareg.getInternArbeidsforholdRef().orElseThrow(), INNTEKTSMELDING);

    }

    /**
     * <pre>
    Arbeidsgiver har meldt opphør for et arbeidsforhold, men arbeidstaker jobber fortsatt- kan være typisk gjennopptak av arbeide etter periode med foreldrepenger og arbeidsgiver  
    betaler og rapporterer lønn på arbeidsforholdet i a-inntekt. Men har glemt å rapportere arbeidstaker som ansatt igjen.
    Her forventer jeg at det skal komme et aksjonspunkt slik at arbeidsgiver kan kontaktes og rapportere arbeidsforholdet som aktivt. Her gikk det rett til avslag
     * </pre>
     */
    @Test
    public void håndter_overstyring_inntektsmelding_mottatt_for_arbeidsforhold_med_inntekt_men_ikke_registrert_i_aareg() throws Exception {
        var arbInfo = ArbeidsforholdInformasjonBuilder.oppdatere(Optional.empty());

        Arbeidsgiver virksomhetIAareg = Arbeidsgiver.virksomhet("972674818");
        Arbeidsgiver virksomhetUtenAareg = Arbeidsgiver.virksomhet("896929119");

        // har bare ene arbeidsgiver i aareg
        var yrkGenerator = new GenererYrkArbeidsforhold(virksomhetIAareg, arbInfo);
        var yrk1 = yrkGenerator.yrkesaktivitetUten();

        // får inntektsmelding fra begge
        var imGenerator = new GenererImArbeidsforhold(arbInfo);
        var im1 = imGenerator.inntektsmeldingUten(virksomhetIAareg);
        var im2 = imGenerator.inntektsmeldingUten(virksomhetUtenAareg);

        // får godkjent lagt til arbeidsforhol der ikke registrert i aareg
        var generereArbInfo = new GenererArbinfoArbeidsforhold(arbInfo);
        var overstyringForArbUtenAareg = generereArbInfo.overstyring(virksomhetUtenAareg, ArbeidsforholdHandlingType.LAGT_TIL_AV_SAKSBEHANDLER);

        var mapper = new ArbeidsforholdMapper(arbInfo.build());

        tjeneste.mapArbeidsforhold(mapper,
            yrkesaktiviteter(yrk1),
            overstyringer(overstyringForArbUtenAareg),
            inntektsmeldinger(im1, im2));

        // matchende
        var data = mapper.getArbeidsforhold();

        verifiserKilder(data, virksomhetIAareg, yrk1.getArbeidsforholdRef(), AAREG, INNTEKTSMELDING);
        verifiserKilder(data, virksomhetUtenAareg, im2.getInternArbeidsforholdRef().orElseThrow(), SAKSBEHANDLER, INNTEKTSMELDING);

        var dtoerUtenAareg = finnDtoerForAngittArbeidsforhold(data, virksomhetUtenAareg, null);

        /* sjekker vi ikke har årsak før vurderinger er lagt til. */
        assertThat(dtoerUtenAareg).noneSatisfy(d -> assertThat(d.getAksjonspunktÅrsaker()).contains(ArbeidsforholdAksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD));

        /* legger på vurderinger. */
        mapper.mapVurderinger(Map.of(virksomhetUtenAareg, Set.of(new ArbeidsforholdMedÅrsak().leggTilÅrsak(AksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD))));

        /* har nå med årsak fra vurdering. */
        assertThat(dtoerUtenAareg).allSatisfy(d -> assertThat(d.getAksjonspunktÅrsaker()).contains(ArbeidsforholdAksjonspunktÅrsak.INNTEKTSMELDING_UTEN_ARBEIDSFORHOLD));

    }

    private void verifiserKilder(Set<InntektArbeidYtelseArbeidsforholdV2Dto> data,
                                 Arbeidsgiver arbeidsgiver,
                                 InternArbeidsforholdRef internRef,
                                 ArbeidsforholdKilde... kilder) {
        var dtoer = finnDtoerForAngittArbeidsforhold(data, arbeidsgiver, internRef);

        assertThat(dtoer).as("Fant ikke arbeidsforhold: " + internRef + ", for arbeidsgiver=" + arbeidsgiver).hasSize(1);
        assertThat(dtoer.stream()).allSatisfy(dto -> assertThat(dto.getKilde()).containsOnly(kilder));

    }

    private List<InntektArbeidYtelseArbeidsforholdV2Dto> finnDtoerForAngittArbeidsforhold(Set<InntektArbeidYtelseArbeidsforholdV2Dto> data,
                                                                                          Arbeidsgiver arbeidsgiver,
                                                                                          InternArbeidsforholdRef internRef) {
        assertThat(data).isNotEmpty();
        var expectedRef = internRef == null ? null : internRef.getUUIDReferanse();
        var dtoer = data.stream()
            .filter(dto -> Objects.equals(arbeidsgiver, dto.getArbeidsgiver())
                && Objects.equals(expectedRef, dto.getArbeidsforhold().getInternArbeidsforholdId()))
            .collect(Collectors.toList());
        return dtoer;
    }

    private EksternArbeidsforholdRef nesteEksternRef() {
        return EksternArbeidsforholdRef.ref("ref-" + counter.incrementAndGet());
    }

    private InternArbeidsforholdRef nesteInternRef() {
        return InternArbeidsforholdRef.nyRef();
    }

}
