package no.nav.ung.sak.formidling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.InformasjonsbrevMalType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.formidling.UtilgjengeligÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.GenereltFritekstBrevDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevForhåndsvisDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevMottakerDto;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevValgDto;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static no.nav.ung.sak.formidling.HtmlAssert.assertThatHtml;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class InformasjonsbrevTjenesteTest {

    @Inject
    private EntityManager entityManager;

    private final PdlKlientFake pdlKlient = PdlKlientFake.medTilfeldigFnr();
    protected String fnr = pdlKlient.fnr();

    private UngTestRepositories ungTestRepositories;
    private InformasjonsbrevTjeneste informasjonsbrevTjeneste;


    @BeforeEach
    void setup() {
        ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        informasjonsbrevTjeneste = new InformasjonsbrevTjeneste(
            ungTestRepositories.repositoryProvider().getBehandlingRepository(),
            ungTestRepositories.repositoryProvider().getPersonopplysningRepository(),
            new InformasjonsbrevGenerererTjeneste(
                ungTestRepositories.repositoryProvider().getBehandlingRepository(),
                new AktørTjeneste(pdlKlient),
                new PdfGenKlient(),
                ungTestRepositories.repositoryProvider().getPersonopplysningRepository()
            )
        );
    }

    @Test
    void skal_få_generelt_fritekstbrev_med_riktige_valg() {
        // Given
        LocalDate fom = LocalDate.of(2024, 12, 1);
        Behandling behandling = lagStandardBehandling(BrevScenarioer.innvilget19år(fom));

        // When
        List<InformasjonsbrevValgDto> informasjonsbrevValg = informasjonsbrevTjeneste.informasjonsbrevValg(behandling.getId());

        // Then
        assertThat(informasjonsbrevValg.size()).isEqualTo(1);
        InformasjonsbrevValgDto first = informasjonsbrevValg.getFirst();
        assertThat(first.malType()).isEqualTo(InformasjonsbrevMalType.GENERELT_FRITEKSTBREV);

        assertThat(first.mottakere()).isEqualTo(List.of(new InformasjonsbrevMottakerDto(
            behandling.getFagsak().getAktørId().getId(),
            IdType.AKTØRID, null))
        );

        assertThat(first.støtterFritekst()).isFalse();
        assertThat(first.støtterTittelOgFritekst()).isTrue();
        assertThat(first.støtterTredjepartsMottaker()).isFalse();
    }


    @Test
    void skal_få_generelt_fritekstbrev_på_avsluttet_behandling() {
        // Given
        LocalDate fom = LocalDate.of(2024, 12, 1);
        Behandling behandling = lagStandardBehandling(BrevScenarioer.innvilget19år(fom));

        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();

        // When
        List<InformasjonsbrevValgDto> informasjonsbrevValg = informasjonsbrevTjeneste.informasjonsbrevValg(behandling.getId());

        // Then
        assertThat(informasjonsbrevValg.size()).isEqualTo(1);
        InformasjonsbrevValgDto first = informasjonsbrevValg.getFirst();
        assertThat(first.malType()).isEqualTo(InformasjonsbrevMalType.GENERELT_FRITEKSTBREV);
    }

    @Test
    void skal_få_utilgjegelig_mottaker_hvis_død() {
        // Given
        LocalDate fom = LocalDate.of(2024, 12, 1);
        UngTestScenario scenario = BrevScenarioer.død19år(fom);

        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medUngTestGrunnlag(scenario)
            .buildOgLagreMedUng(ungTestRepositories);

        // When
        List<InformasjonsbrevValgDto> informasjonsbrevValg = informasjonsbrevTjeneste.informasjonsbrevValg(behandling.getId());

        // Then
        assertThat(informasjonsbrevValg.size()).isEqualTo(1);
        InformasjonsbrevValgDto first = informasjonsbrevValg.getFirst();
        assertThat(first.mottakere()).isEqualTo(List.of(new InformasjonsbrevMottakerDto(
            behandling.getFagsak().getAktørId().getId(),
            IdType.AKTØRID, UtilgjengeligÅrsak.PERSON_DØD))
        );
    }

    @Test
    void skal_forhåndsvise_informasjonsbrev() {
        // Given
        LocalDate fom = LocalDate.of(2024, 12, 1);
        UngTestScenario scenario = BrevScenarioer.innvilget19år(fom);
        Behandling behandling = lagStandardBehandling(scenario);

        // When
        String overskrift = "Dette er en test for forhåndsvisning av informasjonsbrev";
        String brødtekst = "Test brødtekst.";
        GenerertBrev generertBrev = informasjonsbrevTjeneste.forhåndsvis(
            new InformasjonsbrevForhåndsvisDto(
                behandling.getId(), InformasjonsbrevMalType.GENERELT_FRITEKSTBREV,
                new GenereltFritekstBrevDto(overskrift, brødtekst),
                true
                )
            );

        // Then
        var mottaker = generertBrev.mottaker();
        assertThat(mottaker.navn()).isEqualTo(scenario.navn());
        assertThat(mottaker.aktørId().getAktørId()).isEqualTo(behandling.getAktørId().getAktørId());
        assertThat(generertBrev.gjelder()).isEqualTo(mottaker);
        assertThat(generertBrev.malType()).isEqualTo(DokumentMalType.GENERELT_FRITEKSTBREV);
        assertThat(generertBrev.templateType()).isEqualTo(TemplateType.GENERELT_FRITEKSTBREV);

        var forventetFullBrev = InformasjonsbrevVerifikasjon.medHeaderOgFooter(fnr,
            overskrift + " " + brødtekst + " "
        );

        String brevtekst = generertBrev.dokument().html();
        assertThatHtml(brevtekst).asPlainTextIsEqualTo(forventetFullBrev);
        assertThatHtml(brevtekst)
            .containsHtmlSubSequenceOnce(
                "<h1>" + overskrift + "</h1>"
            );

    }

    private Behandling lagStandardBehandling(UngTestScenario scenario) {

        return TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medUngTestGrunnlag(scenario)
            .buildOgLagreMedUng(ungTestRepositories);
    }

}
