package no.nav.foreldrepenger.mottak.vurderfagsystem.fp;

import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.BehandlingslagerTestUtil.lagNavBruker;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.buildFagsakMedUdefinertRelasjon;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggBehandlingMedEndretDato;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggBehandlingUdefinert;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.byggVurderFagsystem;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.fagsakFødselMedId;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ÅPEN_FAGSAK_ID_1;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ÅPEN_FAGSAK_ID_2;
import static no.nav.foreldrepenger.mottak.vurderfagsystem.impl.VurderFagsystemTestUtils.ÅPEN_SAKSNUMMER_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;

import no.nav.foreldrepenger.behandling.BehandlendeFagsystem;
import no.nav.foreldrepenger.behandling.FagsakTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystem;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesTjeneste;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemFellesUtils;
import no.nav.foreldrepenger.mottak.vurderfagsystem.VurderFagsystemTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingTema;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

public class VurderFagsystemTjenesteImplTest {


    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private VurderFagsystemFellesTjeneste vurderFagsystemFellesTjeneste;
    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private FagsakRepository fagsakRepositoryMock;

    private Fagsak fagsakUdefinert = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker());

    private MottatteDokumentTjeneste mottatteDokumentTjenesteMock ;
    private BehandlingRepositoryProvider repositoryProvider;
    private VurderFagsystemFellesUtils fellesUtils;

    private FagsakTjeneste fagsakTjeneste;
    private VurderFagsystemTjeneste vurderFagsystemTjeneste;

    @Before
    public void setUp() {
        mottatteDokumentTjenesteMock = Mockito.mock(MottatteDokumentTjeneste.class);
        repositoryProvider = mock(BehandlingRepositoryProvider.class);
        behandlingRepository = mock(BehandlingRepository.class);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        fagsakRepositoryMock = mock(FagsakRepository.class);
        when(repositoryProvider.getFagsakRepository()).thenReturn(fagsakRepositoryMock);
        fellesUtils = new VurderFagsystemFellesUtils(behandlingRepository, mottatteDokumentTjenesteMock, null);
        fagsakTjeneste = new FagsakTjeneste(repositoryProvider, null);
        vurderFagsystemTjeneste = new VurderFagsystemTjenesteImpl(fellesUtils);

    }

    @Test
    public void nesteStegSkalVæreManuellHvisEndringPåSakFlaggetSkalBehandlesAvInfotrygd() throws Exception {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker(), ÅPEN_SAKSNUMMER_1);
        fagsak.setSkalTilInfotrygd(true);
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.FORELDREPENGER_FØDSEL, true);
        vfData.setDokumentTypeId(DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD);
        vfData.setSaksnummer(fagsak.getSaksnummer());

        vurderFagsystemFellesTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(vurderFagsystemTjeneste));
        when(fagsakRepositoryMock.hentSakGittSaksnummer(any(), anyBoolean())).thenReturn(Optional.of(fagsak));
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(Collections.emptyList());

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

    @Test
    public void nesteStegSkalVæreVLHvisEndringMedSaksnummer() throws Exception {
        Fagsak fagsak = Fagsak.opprettNy(FagsakYtelseType.FORELDREPENGER, lagNavBruker(), ÅPEN_SAKSNUMMER_1);
        VurderFagsystem vfData = byggVurderFagsystem(BehandlingTema.FORELDREPENGER_FØDSEL, true);
        vfData.setDokumentTypeId(DokumentTypeId.FORELDREPENGER_ENDRING_SØKNAD);
        vfData.setSaksnummer(fagsak.getSaksnummer());

        when(fagsakRepositoryMock.hentSakGittSaksnummer(any(), anyBoolean())).thenReturn(Optional.of(fagsak));
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(List.of(fagsak));
        vurderFagsystemFellesTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(vurderFagsystemTjeneste));

        BehandlendeFagsystem result = toVurderFagsystem(vfData);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.VEDTAKSLØSNING);
        assertThat(result.getSaksnummer()).hasValueSatisfying(it -> assertThat(it).isEqualTo(ÅPEN_SAKSNUMMER_1));
    }

    private BehandlendeFagsystem toVurderFagsystem(VurderFagsystem vfData) {
        return vurderFagsystemFellesTjeneste.vurderFagsystem(vfData);

    }

    @Test
    public void skalReturnereManuellBehandlingNårFlereÅpneSakerFinnesPåBruker() {
        VurderFagsystem fagsystem = byggVurderFagsystem(BehandlingTema.FORELDREPENGER, true);
        when(fagsakRepositoryMock.hentJournalpost(any())).thenReturn(Optional.empty());

        List<Fagsak> saksliste = new ArrayList<>();
        saksliste.add(buildFagsakMedUdefinertRelasjon(ÅPEN_FAGSAK_ID_1, false));
        saksliste.add(buildFagsakMedUdefinertRelasjon(ÅPEN_FAGSAK_ID_2, false));

        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(ÅPEN_FAGSAK_ID_1))
            .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_1), 10));

        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(ÅPEN_FAGSAK_ID_2))
            .thenReturn(byggBehandlingMedEndretDato(fagsakFødselMedId(ÅPEN_FAGSAK_ID_2), 12));
        when(fagsakRepositoryMock.hentForBruker(any())).thenReturn(saksliste);

        Optional<Behandling> behandling = Optional.of(byggBehandlingUdefinert(fagsakUdefinert));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(any())).thenReturn(behandling);
        when(behandlingRepository.hentÅpneBehandlingerForFagsakId(any())).thenReturn(List.of(behandling.get()));
        vurderFagsystemFellesTjeneste = new VurderFagsystemFellesTjeneste(fagsakTjeneste, fellesUtils, new UnitTestLookupInstanceImpl<>(vurderFagsystemTjeneste));

        BehandlendeFagsystem result = toVurderFagsystem(fagsystem);
        assertThat(result.getBehandlendeSystem()).isEqualTo(BehandlendeFagsystem.BehandlendeSystem.MANUELL_VURDERING);
    }

}
