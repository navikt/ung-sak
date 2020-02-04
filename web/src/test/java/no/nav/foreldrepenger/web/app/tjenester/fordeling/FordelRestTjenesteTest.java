package no.nav.foreldrepenger.web.app.tjenester.fordeling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.foreldrepenger.mottak.dokumentmottak.impl.DokumentmottakerPleiepengerBarnSoknad;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.dokumentarkiv.journal.JournalTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.JournalpostId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kontrakter.fordel.BehandlendeFagsystemDto;
import no.nav.foreldrepenger.kontrakter.fordel.FagsakInfomasjonDto;
import no.nav.foreldrepenger.mottak.dokumentmottak.SaksbehandlingDokumentmottakTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakOrchestrator;
import no.nav.foreldrepenger.web.app.soap.sak.tjeneste.OpprettSakTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.FordelRestTjeneste.AbacSaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.fordeling.FordelRestTjeneste.AbacVurderFagsystemDto;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;

public class FordelRestTjenesteTest {

    private static final AktørId AKTØR_ID = AktørId.dummy();

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private SaksbehandlingDokumentmottakTjeneste dokumentmottakTjenesteMock;
    private FagsakTjeneste fagsakTjenesteMock;
    private OpprettSakOrchestrator opprettSakOrchestratorMock;
    private OpprettSakTjeneste opprettSakTjenesteMock;

    private VurderFagsystemFellesTjeneste vurderFagsystemTjenesteMock;
    private JournalTjeneste journalTjeneste;

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    private FordelRestTjeneste fordelRestTjeneste;
    private DokumentmottakerPleiepengerBarnSoknad dokumentmottakerPleiepengerBarnSoknad;

    @Before
    public void setup() {
        dokumentmottakTjenesteMock = mock(SaksbehandlingDokumentmottakTjeneste.class);
        journalTjeneste = mock(JournalTjeneste.class);
        fagsakTjenesteMock = new FagsakTjeneste(repositoryProvider, null);
        opprettSakOrchestratorMock = mock(OpprettSakOrchestrator.class);
        opprettSakTjenesteMock = mock(OpprettSakTjeneste.class);
        vurderFagsystemTjenesteMock = mock(VurderFagsystemFellesTjeneste.class);
        dokumentmottakerPleiepengerBarnSoknad = mock(DokumentmottakerPleiepengerBarnSoknad.class);

        fordelRestTjeneste = new FordelRestTjeneste(dokumentmottakTjenesteMock,
            journalTjeneste,
            fagsakTjenesteMock,
            opprettSakOrchestratorMock,
            opprettSakTjenesteMock,
            vurderFagsystemTjenesteMock,
            dokumentmottakerPleiepengerBarnSoknad);
    }

    @Test
    public void skalReturnereFagsystemVedtaksløsning() {
        Saksnummer saksnummer = new Saksnummer("12345");
        AbacVurderFagsystemDto innDto = new AbacVurderFagsystemDto("1234", true, AKTØR_ID.getId(), "ab0047");
        BehandlendeFagsystem behandlendeFagsystem = new BehandlendeFagsystem(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        behandlendeFagsystem = behandlendeFagsystem.medSaksnummer(saksnummer);

        when(vurderFagsystemTjenesteMock.vurderFagsystem(any(VurderFagsystem.class))).thenReturn(behandlendeFagsystem);

        BehandlendeFagsystemDto result = fordelRestTjeneste.vurderFagsystem(innDto);

        assertThat(result).isNotNull();
        assertThat(String.valueOf(result.getSaksnummer().get())).isEqualTo(saksnummer.getVerdi());
        assertThat(result.isBehandlesIVedtaksløsningen()).isTrue();
    }

    @Test
    public void skalReturnereFagsystemManuell() {
        Saksnummer saksnummer = new Saksnummer("12345");
        JournalpostId journalpostId = new JournalpostId("1234");
        AbacVurderFagsystemDto innDto = new AbacVurderFagsystemDto(journalpostId.getVerdi(), false, AKTØR_ID.getId(), "ab0047");
        innDto.setDokumentTypeIdOffisiellKode(DokumentTypeId.INNTEKTSMELDING.getOffisiellKode());
        BehandlendeFagsystem behandlendeFagsystem = new BehandlendeFagsystem(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
        behandlendeFagsystem = behandlendeFagsystem.medSaksnummer(saksnummer);

        when(vurderFagsystemTjenesteMock.vurderFagsystem(any(VurderFagsystem.class))).thenReturn(behandlendeFagsystem);

        BehandlendeFagsystemDto result = fordelRestTjeneste.vurderFagsystem(innDto);

        assertThat(result).isNotNull();
        assertThat(result.isManuellVurdering()).isTrue();
    }

    @Test
    public void skalReturnereNullNårFagsakErStengt() {
        Saksnummer saknr = new Saksnummer("1");
        var scenario = TestScenarioBuilder.builderMedSøknad(AKTØR_ID);
        scenario.medSaksnummer(saknr);
        Behandling behandling = scenario.lagre(repositoryProvider);
        repositoryProvider.getFagsakRepository().fagsakSkalBehandlesAvInfotrygd(behandling.getFagsakId());
        FagsakInfomasjonDto result = fordelRestTjeneste.fagsak(new AbacSaksnummerDto("1"));

        assertThat(result).isNull();
    }

}
