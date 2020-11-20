package no.nav.k9.sak.web.app.tjenester.fagsak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.prosessering.ProsesseringAsynkTjeneste;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.test.util.fagsak.FagsakBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;

public class FagsakApplikasjonTjenesteTest {

    private static final String FNR = "12345678901";
    private static final AktørId AKTØR_ID = AktørId.dummy();
    private static final Saksnummer SAKSNUMMER  = new Saksnummer("123");

    private FagsakApplikasjonTjeneste tjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private TpsTjeneste tpsTjeneste;

    @BeforeEach
    public void oppsett() {
        tpsTjeneste = mock(TpsTjeneste.class);
        fagsakRepository = mock(FagsakRepository.class);
        behandlingRepository = mock(BehandlingRepository.class);

        ProsesseringAsynkTjeneste prosesseringAsynkTjeneste = mock(ProsesseringAsynkTjeneste.class);

        BehandlingRepositoryProvider repositoryProvider = mock(BehandlingRepositoryProvider.class);
        when(repositoryProvider.getFagsakRepository()).thenReturn(fagsakRepository);
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);

        tjeneste = new FagsakApplikasjonTjeneste(repositoryProvider, prosesseringAsynkTjeneste, tpsTjeneste);
    }

    @Test
    public void skal_hente_saker_på_fnr() {
        // Arrange
        Personinfo personinfo = new PersoninfoBuilder().medAktørId(AKTØR_ID).build();
        when(tpsTjeneste.hentBrukerForFnr(new PersonIdent(FNR))).thenReturn(Optional.of(personinfo));

        Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).medBruker(AKTØR_ID).medSaksnummer(SAKSNUMMER).build();
        when(fagsakRepository.hentForBruker(AKTØR_ID)).thenReturn(Collections.singletonList(fagsak));
        when(behandlingRepository.hentSisteBehandlingForFagsakId(anyLong())).thenReturn(Optional.of(Behandling.forFørstegangssøknad(fagsak).build()));

        // Act
        FagsakSamlingForBruker view = tjeneste.hentSaker(FNR);

        // Assert
        assertThat(view.isEmpty()).isFalse();
        assertThat(view.getFagsakInfoer()).hasSize(1);
        FagsakSamlingForBruker.FagsakRad info = view.getFagsakInfoer().get(0);
        assertThat(info.getFagsak()).isEqualTo(fagsak);
    }

    @Test
    public void skal_hente_saker_på_saksreferanse() {
        // Arrange
        Personinfo personinfo = new PersoninfoBuilder().medAktørId(AKTØR_ID).build();
        Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).medBruker(AKTØR_ID).medSaksnummer(SAKSNUMMER).build();
        when(fagsakRepository.hentSakGittSaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));

        when(behandlingRepository.hentSisteBehandlingForFagsakId(anyLong())).thenReturn(Optional.of(Behandling.forFørstegangssøknad(fagsak).build()));
        when(tpsTjeneste.hentBrukerForAktør(AKTØR_ID)).thenReturn(Optional.of(personinfo));

        // Act
        FagsakSamlingForBruker view = tjeneste.hentSaker(SAKSNUMMER.getVerdi());

        // Assert
        assertThat(view.isEmpty()).isFalse();
        assertThat(view.getFagsakInfoer()).hasSize(1);
        FagsakSamlingForBruker.FagsakRad info = view.getFagsakInfoer().get(0);
        assertThat(info.getFagsak()).isEqualTo(fagsak);
    }

    @Test
    public void skal_returnere_tomt_view_når_fagsakens_bruker_er_ukjent_for_tps() {
        // Arrange
        Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).medBruker(AKTØR_ID).medSaksnummer(SAKSNUMMER).build();
        when(fagsakRepository.hentSakGittSaksnummer(SAKSNUMMER)).thenReturn(Optional.of(fagsak));
        when(tpsTjeneste.hentBrukerForAktør(AKTØR_ID)).thenReturn(Optional.empty()); // Ingen treff i TPS

        // Act
        FagsakSamlingForBruker view = tjeneste.hentSaker(SAKSNUMMER.getVerdi());

        // Assert
        assertThat(view.isEmpty()).isTrue();
    }

    @Test
    public void skal_returnere_tomt_view_ved_ukjent_fnr() {
        when(tpsTjeneste.hentBrukerForFnr(new PersonIdent(FNR))).thenReturn(Optional.empty());

        FagsakSamlingForBruker view = tjeneste.hentSaker(FNR);

        assertThat(view.isEmpty()).isTrue();
    }

    @Test
    public void skal_returnere_tomt_view_ved_ukjent_saksnr() {
        when(fagsakRepository.hentSakGittSaksnummer(SAKSNUMMER)).thenReturn(Optional.empty());

        FagsakSamlingForBruker view = tjeneste.hentSaker(SAKSNUMMER.getVerdi());

        assertThat(view.isEmpty()).isTrue();
    }
}
