package no.nav.ung.sak.domene.abakus;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import no.nav.abakus.iaygrunnlag.ArbeidsforholdRefDto;
import no.nav.abakus.iaygrunnlag.Organisasjon;
import no.nav.abakus.iaygrunnlag.Periode;
import no.nav.abakus.iaygrunnlag.arbeidsforhold.v1.ArbeidsforholdDto;
import no.nav.abakus.iaygrunnlag.kodeverk.ArbeidType;
import no.nav.k9.felles.konfigurasjon.konfig.Tid;

public class ArbeidsforholdTjenesteMock {

    private static final String ORGNR1 = "973093681";
    private static final LocalDate PERIODE_FOM = LocalDate.now().minusYears(3L);
    private final ArbeidsforholdTjeneste arbeidsforholdTjeneste;

    public ArbeidsforholdTjenesteMock() throws Exception {
        List<ArbeidsforholdDto> response = opprettResponse();

        AbakusTjeneste arbeidsforholdConsumer = mock(AbakusTjeneste.class);
        when(arbeidsforholdConsumer.hentArbeidsforholdIPerioden(any())).thenReturn(response);
        this.arbeidsforholdTjeneste = new ArbeidsforholdTjeneste(arbeidsforholdConsumer);
    }

    public ArbeidsforholdTjeneste getMock() {
        return arbeidsforholdTjeneste;
    }

    private List<ArbeidsforholdDto> opprettResponse() throws Exception {
        final var arbeidsforhold = new ArbeidsforholdDto(new Organisasjon(ORGNR1), ArbeidType.ORDINÆRT_ARBEIDSFORHOLD);
        arbeidsforhold.setAnsettelsesperiode(List.of(new Periode(PERIODE_FOM, Tid.TIDENES_ENDE)));
        arbeidsforhold.setArbeidsforholdId(new ArbeidsforholdRefDto(null, "1"));

        return List.of(arbeidsforhold);
    }
}
