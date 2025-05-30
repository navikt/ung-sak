package no.nav.ung.sak.formidling;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.EndringProgramPeriodeInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

class EndringProgramPeriodeTest extends AbstractVedtaksbrevInnholdByggerTest {

   EndringProgramPeriodeTest() {
        super(1,
            "Vi har endret ungdomsprogramytelse din");
    }


    @DisplayName("Endret eksisterende sluttdato")
    @Test
    void flytterSluttdatoBakover() {
        LocalDate fomDato = LocalDate.of(2024, 12, 1);
        LocalDate opprinneligOpphørsdato = LocalDate.of(2025, 8, 15);
        LocalDate nyOpphørsdato = LocalDate.of(2025, 8, 10);

        var opphørGrunnlag = BrevScenarioer.endringOpphør(opprinneligOpphørsdato, new LocalDateInterval(fomDato, fomDato.plusWeeks(52)));
        var endringGrunnlag = BrevScenarioer.endringSluttdato(nyOpphørsdato, opphørGrunnlag.programPerioder().getFirst().getPeriode().toLocalDateInterval());
        var behandling = lagEndringScenario(endringGrunnlag, opphørGrunnlag);

        var forventet = VedtaksbrevVerifikasjon.medHeaderOgFooter(fnr,
            """
                Vi har endret ungdomsprogramytelse din \
                Fra 10. august 2025 får du ikke lenger penger gjennom ungdomsprogramytelse. \
                Du fikk tidligere beskjed om at du skulle få ungdomsprogramytelse til og med 14. august 2025, \
                men den datoen gjelder ikke lenger fordi den er endret av din veileder. \
                Derfor har du nå fått en ny dato for når ungdomsprogramytelse din stopper. \
                Vedtaket er gjort etter arbeidsmarkedsloven § xx og forskrift om xxx § xx. \
                """);



        GenerertBrev generertBrev = genererVedtaksbrev(behandling.getId());
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.ENDRING_PROGRAMPERIODE);

        var brevtekst = generertBrev.dokument().html();

        assertThatHtml(brevtekst)
            .asPlainTextIsEqualTo(forventet)
            .containsHtmlSubSequenceOnce(
                "<h1>Vi har endret ungdomsprogramytelse din</h1>"
            );

    }

    private Behandling lagEndringScenario(UngTestScenario ungTestscenario, UngTestScenario forrigeBehandlingScenario) {
        TestScenarioBuilder builder = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(forrigeBehandlingScenario)
            ;
        var originalBehandling = builder.buildOgLagreMedUng(ungTestRepositories);
        originalBehandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        originalBehandling.avsluttBehandling();

        builder
            .medBehandlingType(BehandlingType.REVURDERING)
            .medUngTestGrunnlag(ungTestscenario)
            .medOriginalBehandling(originalBehandling, null);

        var behandling = builder.buildOgLagreNyUngBehandlingPåEksisterendeSak(ungTestRepositories);


        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();


        return behandling;
    }


    @Override
    protected VedtaksbrevInnholdBygger lagVedtaksbrevInnholdBygger() {
        return new EndringProgramPeriodeInnholdBygger(ungTestRepositories.ungdomsprogramPeriodeRepository());
    }

    @Override
    protected Behandling lagScenarioForFellesTester() {
        LocalDate fomDato = LocalDate.of(2024, 12, 1);
        LocalDate opprinnligOpphørsdato = LocalDate.of(2025, 8, 15);
        LocalDate nyOpphørsdato = LocalDate.of(2025, 8, 10);

        var opphørGrunnlag = BrevScenarioer.endringOpphør(opprinnligOpphørsdato, new LocalDateInterval(fomDato, fomDato.plusWeeks(52)));
        var endringGrunnlag = BrevScenarioer.endringSluttdato(nyOpphørsdato, opphørGrunnlag.programPerioder().getFirst().getPeriode().toLocalDateInterval());
        return lagEndringScenario(endringGrunnlag, opphørGrunnlag);
    }
}


