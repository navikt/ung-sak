package no.nav.k9.sak.web.app.tjenester.brukerdialog;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hibernate.internal.util.collections.CollectionHelper.listOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class BrukerdialogTjenesteTest {
    @Mock
    private FagsakRepository fagsakRepository;

    @Mock
    private BehandlingRepository behandlingRepository;

    @InjectMocks
    private BrukerdialogTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

    }

    @Test
    void returnerer_gyldig_vedtak_når_man_har_nøyaktig_ett_innvilget_vedtak() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.OMSORGSPENGER_KS, AktørId.dummy(), new Saksnummer("1234"), LocalDate.now(), LocalDate.now());
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD)
                    .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
                    .medAvsluttetDato(LocalDateTime.now())
                    .build();

        when(fagsakRepository.finnFagsakRelatertTil(any(), any(), any(), any(), any(), any())).thenReturn(listOf(fagsak));
        when(behandlingRepository.finnSisteInnvilgetBehandling(any())).thenReturn(Optional.of(behandling));

        var resultat = tjeneste.harGyldigOmsorgsdagerVedtak(AktørId.dummy(), AktørId.dummy());

        assertThat(resultat.harInnvilgedeBehandlinger()).isTrue();
    }

    @Test
    void returnerer_ugyldig_vedtak_når_man_ikke_har_noe_innvilget_vedtak() {
        var fagsak = Fagsak.opprettNy(FagsakYtelseType.OMSORGSPENGER_KS, AktørId.dummy(), new Saksnummer("1234"), LocalDate.now(), LocalDate.now());
        var behandling = Behandling.nyBehandlingFor(fagsak, BehandlingType.FØRSTEGANGSSØKNAD)
            .medBehandlingResultatType(BehandlingResultatType.AVSLÅTT)
            .medAvsluttetDato(LocalDateTime.now())
            .build();

        when(fagsakRepository.finnFagsakRelatertTil(any(), any(), any(), any(), any(), any())).thenReturn(listOf(fagsak));
        when(behandlingRepository.finnSisteInnvilgetBehandling(any())).thenReturn(Optional.empty());

        var resultat = tjeneste.harGyldigOmsorgsdagerVedtak(AktørId.dummy(), AktørId.dummy());

        assertThat(resultat.harInnvilgedeBehandlinger()).isFalse();
    }

    @Test
    void returnerer_gyldig_vedtak_når_man_har_flere_fagsaker_med_ett_innvilget_vedtak() {
        var INNVILGET_FAGSAK_ID = 1L;
        var fagsakMedInnvilgetBehandling = Fagsak.opprettNy(FagsakYtelseType.OMSORGSPENGER_KS, AktørId.dummy(), new Saksnummer("1234"), LocalDate.now(), LocalDate.now());
        fagsakMedInnvilgetBehandling.setId(INNVILGET_FAGSAK_ID);
        var innvilgetBehandling = Behandling.nyBehandlingFor(fagsakMedInnvilgetBehandling, BehandlingType.FØRSTEGANGSSØKNAD)
            .medBehandlingResultatType(BehandlingResultatType.INNVILGET)
            .medAvsluttetDato(LocalDateTime.now())
            .build();

        var AVSLÅTT_FAGSAK_ID = 0L;
        var fagsakMedAvslåttBehandling = Fagsak.opprettNy(FagsakYtelseType.OMSORGSPENGER_KS, AktørId.dummy(), new Saksnummer("1234"), LocalDate.now(), LocalDate.now());
        fagsakMedAvslåttBehandling.setId(AVSLÅTT_FAGSAK_ID);

        when(fagsakRepository.finnFagsakRelatertTil(any(), any(), any(), any(), any(), any())).thenReturn(listOf(fagsakMedInnvilgetBehandling, fagsakMedAvslåttBehandling));
        when(behandlingRepository.finnSisteInnvilgetBehandling(INNVILGET_FAGSAK_ID)).thenReturn(Optional.of(innvilgetBehandling));
        when(behandlingRepository.finnSisteInnvilgetBehandling(AVSLÅTT_FAGSAK_ID)).thenReturn(Optional.empty());

        var resultat = tjeneste.harGyldigOmsorgsdagerVedtak(AktørId.dummy(), AktørId.dummy());

        assertThat(resultat.harInnvilgedeBehandlinger()).isTrue();
    }
}