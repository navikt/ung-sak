package no.nav.ung.sak.mottak.dokumentmottak;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.mottak.Behandlingsoppretter;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class InnhentDokumentTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private FagsakRepository fagsakRepository;


    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    @Mock
    private Dokumentmottaker dokumentmottaker;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    @Mock
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    private InnhentDokumentTjeneste innhentDokumentTjeneste;
    private DokumentmottakerFelles dokumentmottakerFelles;

    @BeforeEach
    public void oppsett() {
        MockitoAnnotations.initMocks(this);

        dokumentmottakerFelles = Mockito.spy(new DokumentmottakerFelles(
            prosessTaskTjeneste,
            historikkinnslagTjeneste));

        innhentDokumentTjeneste = Mockito.spy(new InnhentDokumentTjeneste(
            new UnitTestLookupInstanceImpl<>(dokumentmottaker),
            dokumentmottakerFelles,
            behandlingsoppretter,
            repositoryProvider,
            behandlingProsesseringTjeneste,
            prosessTaskTjeneste,
            fagsakProsessTaskRepository));

        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(enhet);
        when(behandlingProsesseringTjeneste.opprettTaskGruppeForGjenopptaOppdaterFortsett(any(Behandling.class), anyBoolean(), anyBoolean())).thenReturn(new ProsessTaskGruppe());

        when(dokumentmottaker.getBehandlingÅrsakType(Brevkode.UNGDOMSYTELSE_SOKNAD)).thenReturn(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER);
    }

    @Test
    public void skal_opprette_førstegangsbehandling() {

        Fagsak fagsak = DokumentmottakTestUtil.byggFagsak(AktørId.dummy(), new Saksnummer("123"), fagsakRepository);
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(123L, "", now(), "123", Brevkode.UNGDOMSYTELSE_SOKNAD);
        Behandling førstegangsbehandling = mock(Behandling.class);
        when(førstegangsbehandling.getFagsak()).thenReturn(fagsak);
        when(førstegangsbehandling.getAktørId()).thenReturn(AktørId.dummy());
        when(førstegangsbehandling.getFagsakId()).thenReturn(fagsak.getId());
        when(behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty())).thenReturn(førstegangsbehandling);

        // Act
        innhentDokumentTjeneste.mottaDokument(fagsak, List.of(mottattDokument));

        // Assert
        verify(behandlingsoppretter).opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(mottattDokument), førstegangsbehandling);
    }

}
