package no.nav.ung.sak.web.app.tjenester.fagsak;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.felles.exception.ManglerTilgangException;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.sif.abac.kontrakt.abac.resultat.Tilgangsbeslutning;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.FagsakTjeneste;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.pip.PipRepository;
import no.nav.ung.sak.domene.person.tps.TpsTjeneste;
import no.nav.ung.sak.kontrakt.produksjonsstyring.SøkeSakEllerBrukerDto;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.web.app.tjenester.behandling.BehandlingsoppretterTjeneste;
import no.nav.ung.sak.web.server.abac.SifAbacPdpRestKlient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    private FagsakRestTjeneste tjeneste;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void oppsett() {
        fagsakApplikasjonTjeneste = mock(FagsakApplikasjonTjeneste.class);
        tpsTjeneste = mock(TpsTjeneste.class);
        abacPdpRestKlient = mock(SifAbacPdpRestKlient.class);
        tjeneste = new FagsakRestTjeneste(fagsakApplikasjonTjeneste, mock(FagsakTjeneste.class), mock(BehandlingsoppretterTjeneste.class),
            false, false, mock(BehandlingRepository.class), mock(ProsessTaskTjeneste.class), mock(FagsakRepository.class),
            abacPdpRestKlient, mock(PipRepository.class), tpsTjeneste, mock(Instance.class));
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
