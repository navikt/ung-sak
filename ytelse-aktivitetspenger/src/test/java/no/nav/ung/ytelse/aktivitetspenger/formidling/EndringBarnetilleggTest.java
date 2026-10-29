package no.nav.ung.ytelse.aktivitetspenger.formidling;

import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.GenerertBrev;
import no.nav.ung.sak.formidling.vedtak.resultat.DefaultDetaljertResultatTidslinjeUtleder;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerGrunnlagRepository;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.EndringBarnetilleggDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.EndringBarnetilleggInnholdBygger;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerEndringBarnetilleggScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class EndringBarnetilleggTest extends AbstractAktivitetspengerVedtaksbrevInnholdByggerTest {

    private static final LocalDate FOM = LocalDate.of(2025, 8, 1);

    @Inject
    @Any
    private DefaultDetaljertResultatTidslinjeUtleder detaljertResultatTidslinjeUtleder;

    EndringBarnetilleggTest() {
        super(1, "Du får mer i aktivitetspenger");
    }

    @DisplayName("Fødsel med ett barn gir barnetillegg-brev")
    @Test
    void fødselMedEttBarn() {
        var behandling = lagScenario(AktivitetspengerEndringBarnetilleggScenarioer.fødselMedEttBarn(FOM));

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_BARNETILLEGG);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i aktivitetspenger fordi du har fått barn \
                Du får 37 kroner i barnetillegg per dag fra og med 16. august 2025, utenom lørdag og søndag. \
                Det er fordi du fikk barn denne datoen. \
                Når du har barn, får du et barnetillegg på 37 kroner per dag for hvert barn du har. \
                """ + VedtaksbrevVerifikasjon.hjemmelsetning("12 tredje ledd"));

        assertThatHtml(generertBrev.dokument().html())
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får mer i aktivitetspenger fordi du har fått barn</h1>"
            );
    }

    @DisplayName("Fødsel med to barn gir riktig barnetillegg-brev")
    @Test
    void fødselMedToBarn() {
        var behandling = lagScenario(AktivitetspengerEndringBarnetilleggScenarioer.fødselMedToBarn(FOM));

        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_BARNETILLEGG);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Du får mer i aktivitetspenger fordi du har fått barn \
                Du får 74 kroner i barnetillegg per dag fra og med 16. august 2025, utenom lørdag og søndag. \
                Det er fordi du fikk barn denne datoen. \
                Når du har barn, får du et barnetillegg på 37 kroner per dag for hvert barn du har. \
                """ + VedtaksbrevVerifikasjon.hjemmelsetning("12 tredje ledd"));

        assertThatHtml(generertBrev.dokument().html())
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Du får mer i aktivitetspenger fordi du har fått barn</h1>"
            );
    }

    @DisplayName("Barn født før startdato gir startdato som dato for barnetillegg når toggle er på")
    @Test
    void barnFødtFørStartdatoMedToggle() {
        var behandling = lagScenario(AktivitetspengerEndringBarnetilleggScenarioer.fødselFørStartdato(FOM));

        var dto = byggMedToggleForFødselFørStartdato(behandling);

        assertThat(dto.fom()).isEqualTo(FOM);
        assertThat(dto.barnetillegg()).isEqualTo(37L);
    }

    @DisplayName("Barn født etter startdato gir fødselsdato som dato for barnetillegg når toggle er på")
    @Test
    void barnFødtEtterStartdatoMedToggle() {
        var behandling = lagScenario(AktivitetspengerEndringBarnetilleggScenarioer.fødselMedEttBarn(FOM));

        var dto = byggMedToggleForFødselFørStartdato(behandling);

        assertThat(dto.fom()).isEqualTo(LocalDate.of(2025, 8, 16));
        assertThat(dto.barnetillegg()).isEqualTo(37L);
    }

    private EndringBarnetilleggDto byggMedToggleForFødselFørStartdato(Behandling behandling) {
        var bygger = new EndringBarnetilleggInnholdBygger(new AktivitetspengerGrunnlagRepository(entityManager), true);

        var resultat = bygger.bygg(behandling, detaljertResultatTidslinjeUtleder.utledDetaljertResultat(behandling));

        assertThat(resultat.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_BARNETILLEGG);
        return (EndringBarnetilleggDto) resultat.templateInnholdDto();
    }

    private Behandling lagScenario(AktivitetspengerTestScenario testScenario) {
        AktivitetspengerTestScenarioBuilder scenarioBuilder = AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medAktivitetspengerTestGrunnlag(testScenario);

        var behandling = scenarioBuilder.buildOgLagreMedAktivitspenger(repositories);
        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();
        return behandling;
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        return lagScenario(AktivitetspengerEndringBarnetilleggScenarioer.fødselMedEttBarn(FOM));
    }
}

