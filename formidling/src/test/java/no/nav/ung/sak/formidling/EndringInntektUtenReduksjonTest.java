package no.nav.ung.sak.formidling;

import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.scenarioer.EndringInntektScenarioer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class EndringInntektUtenReduksjonTest extends AbstractVedtaksbrevInnholdByggerTest {

    EndringInntektUtenReduksjonTest() {
        super(1, "Vi har ikke endret ungdomsprogramytelsen din");
    }

    @DisplayName("Endringsbrev inntekt uten reduksjon")
    @Test
    void rapportert10000krRegister0krFastsatt0kr() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var behandling = EndringInntektScenarioer.lagBehandlingMedAksjonspunktKontrollerInntekt(
            EndringInntektScenarioer.endring10000KrInntekt0KrRegisterInntekt_0krFastsatt(fom), ungTestRepositories
        );

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooterManuell(fnr,
            "Vi har ikke endret ungdomsprogramytelsen din " +
                "Du har gitt oss beskjed om at du hadde inntekt i perioden fra 1. september 2025 til 30. september 2025. " +
                "Vi har sjekket inntekten din i A-ordningen, men arbeidsgiveren din har ikke registrert at du hadde inntekt i perioden. " +
                "Vi har derfor kommet frem til at du får full ungdomsprogramytelse for perioden. " +
                "Pengene får du utbetalt innen fire dager. " +
                "Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 11. ");


        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT_UTEN_REDUKSJON);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har ikke endret ungdomsprogramytelsen din</h1>"
            );
    }

    @DisplayName("Endringsbrev inntekt uten reduksjon flere måneder")
    @Test
    void rapportert10000krRegister0krFastsatt0kr_flerePerioder() {
        LocalDate fom = LocalDate.of(2025, 8, 1);
        var behandling = EndringInntektScenarioer.lagBehandlingMedAksjonspunktKontrollerInntekt(
            EndringInntektScenarioer.endring10000KrInntekt0KrRegisterInntekt_0krFastsatt_flere_mnd(fom), ungTestRepositories
        );

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooterManuell(fnr,
            "Vi har ikke endret ungdomsprogramytelsen din " +
                "Du har gitt oss beskjed om at du hadde inntekt i perioden fra 1. september 2025 til 31. oktober 2025. " +
                "Vi har sjekket inntekten din i A-ordningen, men arbeidsgiveren din har ikke registrert at du hadde inntekt i perioden. " +
                "Vi har derfor kommet frem til at du får full ungdomsprogramytelse for perioden. " +
                "Pengene får du utbetalt innen fire dager. " +
                "Vedtaket er gjort etter arbeidsmarkedsloven §§ 12 tredje ledd og 13 fjerde ledd og forskrift om forsøk med ungdomsprogram og ungdomsprogramytelse § 11. ");


        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT_UTEN_REDUKSJON);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har ikke endret ungdomsprogramytelsen din</h1>"
            );
    }


    @Override
    protected Behandling lagScenarioForFellesTester() {
        LocalDate fom = LocalDate.of(2024, 12, 1);

        return EndringInntektScenarioer.lagBehandlingMedAksjonspunktKontrollerInntekt(
            EndringInntektScenarioer.endring10000KrInntekt0KrRegisterInntekt_0krFastsatt(fom), ungTestRepositories
        );
    }
}


