package no.nav.foreldrepenger.dokumentbestiller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dokumentbestiller.dto.BestillBrevDto;
import no.nav.foreldrepenger.dokumentbestiller.kafka.DokumentKafkaBestiller;
import no.nav.foreldrepenger.dokumentbestiller.klient.FormidlingRestKlient;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingDto;
import no.nav.k9.kodeverk.dokument.DokumentMalType;
import no.nav.k9.kodeverk.historikk.HistorikkAktør;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;

public class DokumentBestillerApplikasjonTjenesteTest {
    @Mock
    private HistorikkRepository historikkRepositoryMock;
    @Mock
    private FormidlingRestKlient formidlingRestKlient;

    @Mock
    private DokumentKafkaBestiller dokumentKafkaBestiller;

    private Behandling behandling;
    private BehandlingRepositoryProvider repositoryProvider;
    private DokumentBestillerApplikasjonTjeneste tjeneste;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    private void settOpp(AbstractTestScenario<?> scenario) {
        this.behandling = scenario.lagMocked();
        this.repositoryProvider = scenario.mockBehandlingRepositoryProvider();

        BrevHistorikkinnslag brevHistorikkinnslag = new BrevHistorikkinnslag(historikkRepositoryMock);

        tjeneste = new DokumentBestillerApplikasjonTjeneste(
            repositoryProvider.getBehandlingRepository(),
            brevHistorikkinnslag,
            formidlingRestKlient,
            dokumentKafkaBestiller);
    }

    @Test
    public void skal_bestille_brev_fra_fpformidling() {
        // Arrange
        AbstractTestScenario<?> scenario = TestScenarioBuilder.builderMedSøknad();
        settOpp(scenario);

        DokumentMalType dokumentMalTypeInput = DokumentMalType.INNHENT_DOK;
        HistorikkAktør historikkAktør = HistorikkAktør.SAKSBEHANDLER;
        BestillBrevDto bestillBrevDto = new BestillBrevDto(behandling.getId(), dokumentMalTypeInput, "fritekst");

        // Act
        tjeneste.bestillDokument(bestillBrevDto, historikkAktør, false);

        // Assert
        verify(dokumentKafkaBestiller).bestillBrevFraKafka(bestillBrevDto, historikkAktør);
    }

    @Test
    public void skal_bestille_manuelt_brev_fra_fpformidling() {
        // Arrange
        AbstractTestScenario<?> scenario = TestScenarioBuilder.builderMedSøknad();
        settOpp(scenario);

        DokumentMalType dokumentMalTypeInput = DokumentMalType.INNHENT_DOK;
        HistorikkAktør historikkAktør = HistorikkAktør.SAKSBEHANDLER;
        BestillBrevDto bestillBrevDto = new BestillBrevDto(behandling.getId(), dokumentMalTypeInput, "fritekst");

        // Act
        tjeneste.bestillDokument(bestillBrevDto, historikkAktør, true);

        // Assert
        verify(formidlingRestKlient).bestillDokument(any(DokumentbestillingDto.class));

        ArgumentCaptor<Historikkinnslag> historikkinnslagCaptor = ArgumentCaptor.forClass(Historikkinnslag.class);
        verify(historikkRepositoryMock).lagre(historikkinnslagCaptor.capture());
        Historikkinnslag historikkinnslag = historikkinnslagCaptor.getValue();
        assertThat(historikkinnslag.getType()).isEqualTo(HistorikkinnslagType.BREV_BESTILT);
    }

}
