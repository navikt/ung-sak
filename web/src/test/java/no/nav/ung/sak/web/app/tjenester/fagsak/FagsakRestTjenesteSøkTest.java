package no.nav.ung.sak.web.app.tjenester.fagsak;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.sif.abac.kontrakt.abac.resultat.Tilgangsbeslutning;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.pip.PipRepository;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.kontrakt.behandling.BehandlingOpprettingDto;
import no.nav.ung.sak.kontrakt.behandling.SakRettigheterDto;
import no.nav.ung.sak.kontrakt.behandling.SaksnummerDto;
import no.nav.ung.sak.kontrakt.produksjonsstyring.SøkeSakEllerBrukerDto;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.web.app.tjenester.behandling.BehandlingsoppretterTjeneste;
import no.nav.ung.sak.web.server.abac.SifAbacPdpRestKlient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FagsakRestTjenesteSøkTest {

    private static final String FNR = "12345678901";
    private static final AktørId AKTØR_ID = AktørId.dummy();

    private FagsakApplikasjonTjeneste fagsakApplikasjonTjeneste;
    private TpsTjeneste tpsTjeneste;
    private SifAbacPdpRestKlient abacPdpRestKlient;
    private FagsakTjeneste fagsakTjeneste;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste taskTjeneste;
    private FagsakRepository fagsakRepository;
    private FagsakRestTjeneste tjeneste;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void oppsett() {
        fagsakApplikasjonTjeneste = mock(FagsakApplikasjonTjeneste.class);
        tpsTjeneste = mock(TpsTjeneste.class);
        abacPdpRestKlient = mock(SifAbacPdpRestKlient.class);
        fagsakTjeneste = mock(FagsakTjeneste.class);
        behandlingRepository = mock(BehandlingRepository.class);
        taskTjeneste = mock(ProsessTaskTjeneste.class);
        fagsakRepository = mock(FagsakRepository.class);
        tjeneste = new FagsakRestTjeneste(fagsakApplikasjonTjeneste, fagsakTjeneste, mock(BehandlingsoppretterTjeneste.class),
            false, false, behandlingRepository, taskTjeneste, fagsakRepository,
            abacPdpRestKlient, mock(PipRepository.class), tpsTjeneste, mock(Instance.class));
    }

    @Test
    void fjerning_av_ikke_digital_flagg_sender_alle_åpne_ytelsesbehandlinger_tilbake_til_start() {
        var saksnummer = new Saksnummer("ABC12");
        var fagsak = Fagsak.opprettNyForIkkeDigitalBruker(FagsakYtelseType.UNGDOMSYTELSE, AKTØR_ID, saksnummer, LocalDate.now(), LocalDate.now().plusYears(1));
        fagsak.setId(1L);
        when(fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false)).thenReturn(Optional.of(fagsak));
        var avsluttet = behandling(10L, true);
        var ordinær = behandling(11L, false);
        var kontroll = behandling(12L, false);
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(saksnummer)).thenReturn(List.of(avsluttet, ordinær, kontroll));

        var respons = tjeneste.fjernIkkedigitalFlagg(new SaksnummerDto(saksnummer));

        assertThat(respons.getStatus()).isEqualTo(200);
        verify(fagsakRepository).fjernIkkedigitalFlagg(1L);
        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(taskTjeneste, times(2)).lagre(captor.capture());
        assertThat(captor.getAllValues()).extracting(ProsessTaskData::getBehandlingId).containsExactly("11", "12");
    }

    @Test
    void rettigheter_tilbyr_ikke_kontrollbehandling_som_egen_behandlingstype() {
        var saksnummer = new Saksnummer("ABC13");
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AKTØR_ID, saksnummer, LocalDate.now(), LocalDate.now().plusYears(1));
        fagsak.setId(2L);
        when(fagsakTjeneste.finnFagsakGittSaksnummer(saksnummer, false)).thenReturn(Optional.of(fagsak));

        var respons = tjeneste.hentRettigheter(new SaksnummerDto(saksnummer));

        var typer = ((SakRettigheterDto) respons.getEntity()).getBehandlingTypeKanOpprettes().stream()
            .map(BehandlingOpprettingDto::getBehandlingType)
            .toList();
        assertThat(typer).containsExactlyInAnyOrder(BehandlingType.FØRSTEGANGSSØKNAD, BehandlingType.REVURDERING, BehandlingType.KLAGE);
    }

    private static Behandling behandling(Long id, boolean ferdigbehandlet) {
        var behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(id);
        when(behandling.erYtelseBehandling()).thenReturn(true);
        when(behandling.erStatusFerdigbehandlet()).thenReturn(ferdigbehandlet);
        return behandling;
    }

    @Test
    void mangler_tilgang_til_person_gir_ikke_tilgang_uten_å_se_på_saker() {
        Personinfo personinfo = new PersoninfoBuilder().medAktørId(AKTØR_ID).build();
        when(tpsTjeneste.hentBrukerForFnr(new PersonIdent(FNR), FagsakYtelseType.UNGDOMSYTELSE)).thenReturn(Optional.of(personinfo));
        when(abacPdpRestKlient.sjekkTilgangTilPersonerForInnloggetBrukerUngOgAktivitetspenger(any())).thenReturn(new Tilgangsbeslutning(false, Set.of()));

        assertThatThrownBy(() -> tjeneste.søkFagsaker(new SøkeSakEllerBrukerDto(FNR))).isInstanceOf(ManglerTilgangException.class)
            .hasMessageContaining("UNG-452972");

        verifyNoInteractions(fagsakApplikasjonTjeneste);
    }

    @Test
    void tilgang_til_person_gir_oppslag_av_saker() {
        Personinfo personinfo = new PersoninfoBuilder().medAktørId(AKTØR_ID).build();
        when(tpsTjeneste.hentBrukerForFnr(new PersonIdent(FNR), FagsakYtelseType.UNGDOMSYTELSE)).thenReturn(Optional.of(personinfo));
        when(abacPdpRestKlient.sjekkTilgangTilPersonerForInnloggetBrukerUngOgAktivitetspenger(any())).thenReturn(new Tilgangsbeslutning(true, Set.of()));
        when(fagsakApplikasjonTjeneste.hentSakerForBruker(personinfo)).thenReturn(FagsakSamlingForBruker.emptyView());

        assertThat(tjeneste.søkFagsaker(new SøkeSakEllerBrukerDto(FNR))).isEmpty();

        verify(fagsakApplikasjonTjeneste).hentSakerForBruker(personinfo);
    }

    @Test
    void ukjent_fnr_gir_tomt_resultat_uten_tilgangssjekk() {
        when(tpsTjeneste.hentBrukerForFnr(new PersonIdent(FNR), FagsakYtelseType.UNGDOMSYTELSE)).thenReturn(Optional.empty());

        assertThat(tjeneste.søkFagsaker(new SøkeSakEllerBrukerDto(FNR))).isEmpty();

        verifyNoInteractions(abacPdpRestKlient, fagsakApplikasjonTjeneste);
    }

    @Test
    void oppgitt_ytelsetype_brukes_ved_oppslag_av_person() {
        when(tpsTjeneste.hentBrukerForFnr(new PersonIdent(FNR), FagsakYtelseType.AKTIVITETSPENGER)).thenReturn(Optional.empty());

        assertThat(tjeneste.søkFagsaker(new SøkeSakEllerBrukerDto(FNR, FagsakYtelseType.AKTIVITETSPENGER))).isEmpty();

        verify(tpsTjeneste).hentBrukerForFnr(new PersonIdent(FNR), FagsakYtelseType.AKTIVITETSPENGER);
    }
}
