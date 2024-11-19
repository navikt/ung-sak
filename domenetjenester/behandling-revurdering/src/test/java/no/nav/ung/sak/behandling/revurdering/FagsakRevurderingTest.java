package no.nav.ung.sak.behandling.revurdering;


import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.test.util.Whitebox;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FagsakRevurderingTest {

    @Mock
    private BehandlingRepository behandlingRepository;
    private Behandling behandling;
    private Behandling nyesteBehandling;
    private Behandling eldreBehandling;
    private Fagsak fagsak;
    private Saksnummer fagsakSaksnummer  = new Saksnummer("1");

    private Fagsak fagsakMedFlereBehandlinger;
    private Saksnummer fagsakMedFlereBehSaksnr  = new Saksnummer("2");

    @BeforeEach
    public void setup(){
        // behandlingRepository = mock(BehandlingRepository.class);
    }

    @BeforeEach
    public void opprettBehandlinger() {
        fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).medSaksnummer(fagsakSaksnummer).build();
        behandling = Behandling.forFørstegangssøknad(fagsak).build();

        Whitebox.setInternalState(behandling, "id", 1L);

        fagsakMedFlereBehandlinger = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).medSaksnummer(fagsakMedFlereBehSaksnr).build();
        nyesteBehandling = Behandling.forFørstegangssøknad(fagsakMedFlereBehandlinger)
            .medAvsluttetDato(LocalDateTime.now())
            .build();
        eldreBehandling = Behandling.forFørstegangssøknad(fagsakMedFlereBehandlinger)
            .medAvsluttetDato(LocalDateTime.now().minusDays(1))
            .build();

        when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(fagsakSaksnummer))
            .thenReturn(singletonList(behandling));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(fagsakMedFlereBehSaksnr))
            .thenReturn(asList(nyesteBehandling, eldreBehandling));
    }

    @Test
    public void kanIkkeOppretteRevurderingNårÅpenBehandling() throws Exception {
        when(behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(anyLong())).thenReturn(Arrays.asList(behandling));

        FagsakRevurdering tjeneste = new FagsakRevurdering(behandlingRepository);
        Boolean kanRevurderingOpprettes = tjeneste.kanRevurderingOpprettes(fagsak);
        assertThat(kanRevurderingOpprettes).isFalse();
    }

    @Test
    public void kanIkkeOppretteRevurderingNårBehandlingErHenlagt() {
        behandling.setBehandlingResultatType(BehandlingResultatType.HENLAGT_SØKNAD_TRUKKET);

        FagsakRevurdering tjeneste = new FagsakRevurdering(behandlingRepository);
        Boolean kanRevurderingOpprettes = tjeneste.kanRevurderingOpprettes(fagsak);

        assertThat(kanRevurderingOpprettes).isFalse();
    }

    @Test
    public void kanIkkeOppretteRevurderingNårOrigBehandlingErUnntaksbehandling() {
        var unntaksbehandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.UNNTAKSBEHANDLING).build();

        when(behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(fagsakSaksnummer))
            .thenReturn(singletonList(unntaksbehandling));

        FagsakRevurdering tjeneste = new FagsakRevurdering(behandlingRepository);
        Boolean kanRevurderingOpprettes = tjeneste.kanRevurderingOpprettes(fagsak);

        assertThat(kanRevurderingOpprettes).isFalse();
    }

    @Test
    public void behandlingerSkalSorteresSynkendePåAvsluttetDato(){
        Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).build();
        LocalDateTime now = LocalDateTime.now();
        Behandling nyBehandling = Behandling.forFørstegangssøknad(fagsak).medAvsluttetDato(now).build();
        Behandling gammelBehandling = Behandling.forFørstegangssøknad(fagsak).medAvsluttetDato(now.minusDays(1)).build();

        FagsakRevurdering.BehandlingAvsluttetDatoComparator behandlingAvsluttetDatoComparator = new FagsakRevurdering.BehandlingAvsluttetDatoComparator();

        List<Behandling> behandlinger = asList(nyBehandling, gammelBehandling);
        List<Behandling> sorterteBehandlinger = behandlinger.stream().sorted(behandlingAvsluttetDatoComparator).collect(Collectors.toList());

        assertThat(sorterteBehandlinger.get(0).getAvsluttetDato()).isEqualTo(now.withNano(0));
    }

    @Test
    public void behandlingerSkalSorteresSynkendePåOpprettetDatoNårAvsluttetDatoErNull(){
        Fagsak fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).build();
        LocalDateTime now = LocalDateTime.now();
        Behandling nyBehandling = Behandling.forFørstegangssøknad(fagsak).medAvsluttetDato(null).medOpprettetDato(now).build();
        Behandling gammelBehandling = Behandling.forFørstegangssøknad(fagsak).medAvsluttetDato(now).medOpprettetDato(now.minusDays(1)).build();

        FagsakRevurdering.BehandlingAvsluttetDatoComparator behandlingAvsluttetDatoComparator = new FagsakRevurdering.BehandlingAvsluttetDatoComparator();

        List<Behandling> behandlinger = asList(nyBehandling, gammelBehandling);
        List<Behandling> sorterteBehandlinger = behandlinger.stream().sorted(behandlingAvsluttetDatoComparator).collect(Collectors.toList());

        assertThat(sorterteBehandlinger.get(0).getAvsluttetDato()).isNull();
        assertThat(sorterteBehandlinger.get(0).getOpprettetDato()).isEqualTo(now.withNano(0));
    }
}
