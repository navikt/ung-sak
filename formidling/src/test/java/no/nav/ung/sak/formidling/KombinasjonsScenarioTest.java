package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.scenarioer.FørstegangsbehandlingScenarioer;
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

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Disse testene sjekker om det bestilles flere brev på en behandling
 */
@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class KombinasjonsScenarioTest {

    @Inject
    EntityManager entityManager;

    @Inject
    VedtaksbrevTjeneste vedtaksbrevTjeneste;


    @Test
    void endringAvInntektOgFødselAvBarn_lagerBrev() {
        LocalDate fom = LocalDate.of(2025, 11, 1);
        LocalDate barnFødselsdato = fom.withDayOfMonth(1).plusMonths(1).withDayOfMonth(15);
        LocalDate rapportertInntektFom = fom.withDayOfMonth(1).plusMonths(1);

        UngTestScenario ungTestScenario = KombinasjonScenarioer
            .kombinasjon_endringMedInntektOgFødselAvBarn(fom);


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

        var endringInntektBrev = generertBrev.stream().filter(it -> it.malType() == DokumentMalType.ENDRING_INNTEKT).findFirst().orElseThrow();
        assertThatHtml(endringInntektBrev.dokument().html())
            .asPlainTextContains(BrevTestUtils.brevDatoString(rapportertInntektFom));

        var endringHøySatsBrev = generertBrev.stream().filter(it -> it.malType() == DokumentMalType.ENDRING_BARNETILLEGG).findFirst().orElseThrow();
        assertThatHtml(endringHøySatsBrev.dokument().html())
            .asPlainTextContains(BrevTestUtils.brevDatoString(barnFødselsdato));

    }


    @Test
    void endringAvInntektOgOvergangHøySats_lagerBrev() {

        LocalDate fødselsdato = LocalDate.of(2000, 3, 25);
        String rapportertInntektFom = "1. mars 2025";

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
        var endringInntektBrev = generertBrev.stream().filter(it -> it.malType() == DokumentMalType.ENDRING_INNTEKT).findFirst().orElseThrow();
        assertThatHtml(endringInntektBrev.dokument().html()).asPlainTextContains(rapportertInntektFom);

        var endringHøySatsBrev = generertBrev.stream().filter(it -> it.malType() == DokumentMalType.ENDRING_HØY_SATS).findFirst().orElseThrow();
        assertThatHtml(endringHøySatsBrev.dokument().html()).asPlainTextContains(BrevTestUtils.brevDatoString(fødselsdato.plusYears(25)));



    }

    @Test
    void førstegangsbehandlingOgBarn_lagerEttBrev() {

        LocalDate fom = LocalDate.of(2025, 11, 1);
        UngTestScenario ungTestScenario = KombinasjonScenarioer.kombinasjon_førstegangsBehandlingOgBarn(fom);

        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        var behandling = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestScenario)
            .buildOgLagreMedUng(ungTestRepositories);

        behandling.avsluttBehandling();

        List<GenerertBrev> genererteBrev = vedtaksbrevTjeneste.forhåndsvis(new VedtaksbrevForhåndsvisRequest(
            behandling.getId(),
            null,
            true,
            null));

        assertThat(genererteBrev).hasSize(1);
        var generertBrev = genererteBrev.getFirst();
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.INNVILGELSE);
        assertThat(generertBrev.malType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);
        assertThat(generertBrev.dokument().html()).isNotBlank();
        assertThatHtml(generertBrev.dokument().html()).asPlainTextContains("1. november 2025");
    }

    @Test
    void endringStartDatoOgHøySats_lagerBrev() {
        LocalDate opprinneligStartdato = LocalDate.of(2025, 8, 15);
        LocalDate nyStartdato = LocalDate.of(2025, 8, 5);
        var forrigeBehandlingScenario = FørstegangsbehandlingScenarioer.innvilget19år(opprinneligStartdato);
        LocalDate fødselsdato = LocalDate.of(2000, 8, 25);

        var ungTestScenario = KombinasjonScenarioer.kombinasjon_endringStartDatoOgEndringHøySats(fødselsdato, nyStartdato,
            forrigeBehandlingScenario.programPerioder().getFirst().getPeriode().toLocalDateInterval());

        var ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        var builder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(forrigeBehandlingScenario);

        var originalBehandling = builder.buildOgLagreMedUng(ungTestRepositories);
        originalBehandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        originalBehandling.avsluttBehandling();

        builder
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestScenario)
            .medOriginalBehandling(originalBehandling, null);

        var behandling = builder.buildOgLagreNyUngBehandlingPåEksisterendeSak(ungTestRepositories);
        behandling.avsluttBehandling();

        List<GenerertBrev> generertBrev = vedtaksbrevTjeneste.forhåndsvis(new VedtaksbrevForhåndsvisRequest(
            behandling.getId(),
            null,
            true,
            null));

        assertThat(generertBrev).hasSize(2);

        var endringProgramPeriodeBrev = generertBrev.stream().filter(it -> it.malType() == DokumentMalType.ENDRING_PROGRAMPERIODE).findFirst().orElseThrow();
        assertThatHtml(endringProgramPeriodeBrev.dokument().html())
            .asPlainTextContains(BrevTestUtils.brevDatoString(nyStartdato));

        var endringHøySatsBrev = generertBrev.stream().filter(it -> it.malType() == DokumentMalType.ENDRING_HØY_SATS).findFirst().orElseThrow();
        assertThatHtml(endringHøySatsBrev.dokument().html())
            .asPlainTextContains(BrevTestUtils.brevDatoString(fødselsdato.plusYears(25)));

    }

}
