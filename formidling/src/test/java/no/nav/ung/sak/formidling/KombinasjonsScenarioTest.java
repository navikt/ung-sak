package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.scenarioer.KombinasjonScenarioer;
import no.nav.ung.sak.formidling.vedtak.VedtaksbrevTjeneste;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevForhåndsvisRequest;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class KombinasjonsScenarioTest {

    @Inject
    EntityManager entityManager;

    @Inject
    VedtaksbrevTjeneste vedtaksbrevTjeneste;


    @Test
    void endringAvInntektOgFødselAvBarn_lagerBrev() {
        UngTestScenario ungTestScenario = KombinasjonScenarioer
            .kombinasjon_endringMedInntektOgFødselAvBarn(LocalDate.of(2025, 11, 1));

        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        var behandling = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestScenario)
            .buildOgLagreMedUng(ungTestRepositories);

        behandling.avsluttBehandling();

        List<GenerertBrev> generertBrev = vedtaksbrevTjeneste.forhåndsvis(new VedtaksbrevForhåndsvisRequest(
            behandling.getId(),
            null,
            true,
            null));

        assertThat(generertBrev).hasSize(2);
        assertThat(generertBrev)
            .anySatisfy(brev -> {
                assertThat(brev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);
                assertThat(brev.malType()).isEqualTo(DokumentMalType.ENDRING_INNTEKT);
                assertThat(brev.dokument().html()).isNotBlank();
            })
            .anySatisfy(brev -> {
                assertThat(brev.templateType()).isEqualTo(TemplateType.ENDRING_BARNETILLEGG);
                assertThat(brev.malType()).isEqualTo(DokumentMalType.ENDRING_BARNETILLEGG);
                assertThat(brev.dokument().html()).isNotBlank();
            });
    }


    @Test
    void endringAvInntektOgOvergangHøySats_lagerBrev() {

        LocalDate fødselsdato = LocalDate.of(2000, 3, 25);

        UngTestScenario ungTestScenario = KombinasjonScenarioer.kombinasjon_endringMedInntektOgEndringHøySats(fødselsdato);

        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        var behandling = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestScenario)
            .buildOgLagreMedUng(ungTestRepositories);

        behandling.avsluttBehandling();

        List<GenerertBrev> generertBrev = vedtaksbrevTjeneste.forhåndsvis(new VedtaksbrevForhåndsvisRequest(
            behandling.getId(),
            null,
            true,
            null));

        assertThat(generertBrev).hasSize(2);
        assertThat(generertBrev)
            .anySatisfy(brev -> {
                assertThat(brev.templateType()).isEqualTo(TemplateType.ENDRING_INNTEKT);
                assertThat(brev.malType()).isEqualTo(DokumentMalType.ENDRING_INNTEKT);
                assertThat(brev.dokument().html()).isNotBlank();
            })
            .anySatisfy(brev -> {
                assertThat(brev.templateType()).isEqualTo(TemplateType.ENDRING_HØY_SATS);
                assertThat(brev.malType()).isEqualTo(DokumentMalType.ENDRING_HØY_SATS);
                assertThat(brev.dokument().html()).isNotBlank();
            });
    }

}
