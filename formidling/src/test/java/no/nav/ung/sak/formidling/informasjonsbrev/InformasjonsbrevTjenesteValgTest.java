package no.nav.ung.sak.formidling.informasjonsbrev;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.formidling.IdType;
import no.nav.ung.kodeverk.formidling.UtilgjengeligÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.formidling.BrevTestUtils;
import no.nav.ung.sak.formidling.PdlKlientFake;
import no.nav.ung.sak.formidling.mottaker.BrevMottakerTjeneste;
import no.nav.ung.sak.formidling.pdfgen.PdfGenKlient;
import no.nav.ung.sak.formidling.scenarioer.BrevScenarioerUtils;
import no.nav.ung.sak.formidling.scenarioer.EndringProgramPeriodeScenarioer;
import no.nav.ung.sak.formidling.scenarioer.FørstegangsbehandlingScenarioer;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevMottakerValgResponse;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevValgDto;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class InformasjonsbrevTjenesteValgTest {

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
            new InformasjonsbrevGenerererTjeneste(
                ungTestRepositories.repositoryProvider().getBehandlingRepository(),
                new PdfGenKlient(),
                new BrevMottakerTjeneste(new AktørTjeneste(pdlKlient),
                    ungTestRepositories.repositoryProvider().getPersonopplysningRepository()),
                null),
            null,
            ungTestRepositories.repositoryProvider().getPersonopplysningRepository()
        );
    }

    @Test
    void skal_få_generelt_fritekstbrev_med_riktige_valg() {
        // Given
        LocalDate fom = LocalDate.of(2024, 12, 1);
        UngTestScenario scenario = FørstegangsbehandlingScenarioer.innvilget19år(fom);
        Behandling behandling = lagStandardBehandling(scenario);

        // When
        List<InformasjonsbrevValgDto> informasjonsbrevValg = informasjonsbrevTjeneste.informasjonsbrevValg(behandling.getId());

        // Then
        assertThat(informasjonsbrevValg.size()).isEqualTo(1);
        InformasjonsbrevValgDto first = informasjonsbrevValg.getFirst();
        assertThat(first.malType().getKilde()).isEqualTo(DokumentMalType.GENERELT_FRITEKSTBREV);

        assertThat(first.mottakere()).hasSize(1);
        InformasjonsbrevMottakerValgResponse mottaker = first.mottakere().getFirst();
        assertThat(mottaker.id()).isEqualTo(behandling.getFagsak().getAktørId().getId());
        assertThat(mottaker.idType()).isEqualTo(IdType.AKTØRID);
        assertThat(mottaker.navn()).isEqualTo(BrevScenarioerUtils.DEFAULT_NAVN);
        assertThat(mottaker.fødselsdato()).isEqualTo(scenario.fødselsdato());
        assertThat(mottaker.utilgjengeligÅrsak()).isNull();

        assertThat(first.støtterFritekst()).isFalse();
        assertThat(first.støtterTittelOgFritekst()).isTrue();
    }


    @Test
    void skal_få_generelt_fritekstbrev_på_avsluttet_behandling() {
        // Given
        LocalDate fom = LocalDate.of(2024, 12, 1);
        Behandling behandling = lagStandardBehandling(FørstegangsbehandlingScenarioer.innvilget19år(fom));

        behandling.setBehandlingResultatType(BehandlingResultatType.INNVILGET);
        behandling.avsluttBehandling();

        // When
        List<InformasjonsbrevValgDto> informasjonsbrevValg = informasjonsbrevTjeneste.informasjonsbrevValg(behandling.getId());

        // Then
        assertThat(informasjonsbrevValg.size()).isEqualTo(1);
        InformasjonsbrevValgDto first = informasjonsbrevValg.getFirst();
        assertThat(first.malType().getKilde()).isEqualTo(DokumentMalType.GENERELT_FRITEKSTBREV);
    }

    @Test
    void skal_få_utilgjegelig_mottaker_hvis_død() {
        // Given
        LocalDate fom = LocalDate.of(2024, 12, 1);
        UngTestScenario scenario = EndringProgramPeriodeScenarioer.død19år(fom);

        Behandling behandling = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medUngTestGrunnlag(scenario)
            .buildOgLagreMedUng(ungTestRepositories);

        // When
        List<InformasjonsbrevValgDto> informasjonsbrevValg = informasjonsbrevTjeneste.informasjonsbrevValg(behandling.getId());

        // Then
        assertThat(informasjonsbrevValg.size()).isEqualTo(1);
        InformasjonsbrevValgDto first = informasjonsbrevValg.getFirst();
        assertThat(first.mottakere()).isEqualTo(List.of(new InformasjonsbrevMottakerValgResponse(
            behandling.getFagsak().getAktørId().getId(),
            IdType.AKTØRID,
            scenario.fødselsdato(), BrevScenarioerUtils.DEFAULT_NAVN,
            UtilgjengeligÅrsak.PERSON_DØD)));
    }

    private Behandling lagStandardBehandling(UngTestScenario scenario) {

        return TestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medUngTestGrunnlag(scenario)
            .buildOgLagreMedUng(ungTestRepositories);
    }

}
