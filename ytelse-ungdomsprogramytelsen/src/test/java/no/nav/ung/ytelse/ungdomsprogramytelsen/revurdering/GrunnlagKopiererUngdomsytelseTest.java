package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.startdato.StartdatoRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.uttalelse.UttalelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.test.util.Whitebox;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrunnlagKopiererUngdomsytelseTest {

    private final UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository = mock(UngdomsytelseGrunnlagRepository.class);
    private final InntektArbeidYtelseTjeneste iayTjeneste = mock(InntektArbeidYtelseTjeneste.class);
    private GrunnlagKopiererUngdomsytelse kopierer;
    private Behandling original;

    @BeforeEach
    void setUp() {
        var repositoryProvider = mock(BehandlingRepositoryProvider.class);
        when(repositoryProvider.getPersonopplysningRepository()).thenReturn(mock(PersonopplysningRepository.class));
        kopierer = new GrunnlagKopiererUngdomsytelse(repositoryProvider, iayTjeneste, mock(UngdomsprogramPeriodeRepository.class),
            mock(StartdatoRepository.class), mock(TilkjentYtelseRepository.class), mock(UttalelseRepository.class), ungdomsytelseGrunnlagRepository);
        original = lagBehandling(BehandlingType.FØRSTEGANGSSØKNAD, 1L);
    }

    @Test
    void kontrollbehandling_får_kopi_av_sats_og_uttak_fra_originalbehandlingen() {
        var kontrollbehandling = lagBehandling(BehandlingType.KONTROLL, 2L);

        kopierer.kopierGrunnlagVedAutomatiskOpprettelse(original, kontrollbehandling);

        verify(ungdomsytelseGrunnlagRepository).erstattGrunnlagMedKopiFra(1L, 2L);
        verify(iayTjeneste).kopierGrunnlagFraEksisterendeBehandling(1L, 2L);
    }

    @Test
    void revurdering_beregner_sats_og_uttak_selv() {
        var revurdering = lagBehandling(BehandlingType.REVURDERING, 2L);

        kopierer.kopierGrunnlagVedAutomatiskOpprettelse(original, revurdering);

        verify(ungdomsytelseGrunnlagRepository, never()).erstattGrunnlagMedKopiFra(anyLong(), anyLong());
    }

    private static Behandling lagBehandling(BehandlingType type, Long id) {
        var fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.UNGDOMSYTELSE).build();
        var behandling = Behandling.nyBehandlingFor(fagsak, type).build();
        Whitebox.setInternalState(behandling, "id", id);
        return behandling;
    }
}
