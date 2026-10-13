package no.nav.ung.ytelse.aktivitetspenger.formidling.informasjonsbrev;

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
import no.nav.ung.sak.formidling.informasjonsbrev.InformasjonsbrevTjeneste;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevMottakerValgResponse;
import no.nav.ung.sak.kontrakt.formidling.informasjonsbrev.InformasjonsbrevValgDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.BrevTestUtils;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerBrevScenarioerUtils;
import no.nav.ung.ytelse.aktivitetspenger.formidling.scenarioer.AktivitetspengerFørstegangsbehandlingScenarioer;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestRepositories;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenario;
import no.nav.ung.ytelse.aktivitetspenger.testdata.AktivitetspengerTestScenarioBuilder;
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

    private AktivitetspengerTestRepositories repositories;

    @Inject
    private InformasjonsbrevTjeneste informasjonsbrevTjeneste;


    @BeforeEach
    void setup() {
        repositories = BrevTestUtils.lagAlleTestRepositories(entityManager);
    }

    @Test
    void skal_få_generelt_fritekstbrev_med_riktige_valg() {
        // Given
        LocalDate fom = LocalDate.of(2025, 8, 1);
        AktivitetspengerTestScenario scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetKunLavSats(fom);
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
        assertThat(mottaker.navn()).isEqualTo(AktivitetspengerBrevScenarioerUtils.DEFAULT_NAVN);
        assertThat(mottaker.fødselsdato()).isEqualTo(scenario.fødselsdato());
        assertThat(mottaker.utilgjengeligÅrsak()).isNull();

        assertThat(first.støtterFritekst()).isFalse();
        assertThat(first.støtterTittelOgFritekst()).isTrue();
    }


    @Test
    void skal_få_generelt_fritekstbrev_på_avsluttet_behandling() {
        // Given
        LocalDate fom = LocalDate.of(2025, 8, 1);
        Behandling behandling = lagStandardBehandling(AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetKunLavSats(fom));

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
        LocalDate fom = LocalDate.of(2025, 8, 1);
        AktivitetspengerTestScenario scenario = AktivitetspengerFørstegangsbehandlingScenarioer.innvilgetKunLavSatsDød(fom);
        Behandling behandling = lagStandardBehandling(scenario);

        // When
        List<InformasjonsbrevValgDto> informasjonsbrevValg = informasjonsbrevTjeneste.informasjonsbrevValg(behandling.getId());

        // Then
        assertThat(informasjonsbrevValg.size()).isEqualTo(1);
        InformasjonsbrevValgDto first = informasjonsbrevValg.getFirst();
        assertThat(first.mottakere()).isEqualTo(List.of(new InformasjonsbrevMottakerValgResponse(
            behandling.getFagsak().getAktørId().getId(),
            IdType.AKTØRID,
            scenario.fødselsdato(), AktivitetspengerBrevScenarioerUtils.DEFAULT_NAVN,
            UtilgjengeligÅrsak.PERSON_DØD)));
    }

    private Behandling lagStandardBehandling(AktivitetspengerTestScenario scenario) {
        return AktivitetspengerTestScenarioBuilder.builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medAktivitetspengerTestGrunnlag(scenario)
            .buildOgLagreMedAktivitspenger(repositories);
    }

}
