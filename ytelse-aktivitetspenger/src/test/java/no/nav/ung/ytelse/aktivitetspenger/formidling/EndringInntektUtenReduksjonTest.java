package no.nav.ung.ytelse.aktivitetspenger.formidling;

import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerEndringInntektScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class EndringInntektUtenReduksjonTest extends AbstractAktivitetspengerVedtaksbrevInnholdByggerTest {

    private static final LocalDate FOM = LocalDate.of(2025, 8, 1);

    EndringInntektUtenReduksjonTest() {
        super(1, "Vi har ikke endret aktivitetspengene dine");
    }

    @DisplayName("Endringsbrev inntekt uten reduksjon")
    @Test
    void rapportert10000krRegister0krFastsatt0kr() {
        var behandling = lagScenarioMedKontrollerInntektAksjonspunkt(
            AktivitetspengerEndringInntektScenarioer.endring10000KrInntekt0KrRegisterInntekt_0krFastsatt(FOM));

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_INNTEKT_UTEN_REDUKSJON);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooterManuell(fnr,
            "Vi har ikke endret aktivitetspengene dine " +
                "Du har gitt oss beskjed om at du hadde inntekt i perioden fra 1. september 2025 til 30. september 2025. " +
                "Vi har sjekket inntekten din i Aa-registeret, men arbeidsgiveren din har ikke registrert at du hadde en inntekt i perioden. " +
                "Vi har derfor kommet frem til at du får full utbetaling av aktivitetspenger for perioden. " +
                "Pengene får du utbetalt innen fire dager. " +
                VedtaksbrevVerifikasjon.hjemmelsetningFlereParagrafer("10–14"));

        assertThatHtml(generertBrev.dokument().html())
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har ikke endret aktivitetspengene dine</h1>",
                "folketrygdloven <span class=\"ikke-bryt\">§ 25-13</span> tredje ledd",
                "Trøndelag <span class=\"ikke-bryt\">§§ 10–14</span>."
            );
    }

    private Behandling lagScenarioMedKontrollerInntektAksjonspunkt(AktivitetspengerTestScenario testScenario) {
        return AktivitetspengerEndringInntektScenarioer.lagBehandlingMedAksjonspunktKontrollerInntekt(
            testScenario, repositories);
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        return lagScenarioMedKontrollerInntektAksjonspunkt(
            AktivitetspengerEndringInntektScenarioer.endring10000KrInntekt0KrRegisterInntekt_0krFastsatt(FOM));
    }
}
