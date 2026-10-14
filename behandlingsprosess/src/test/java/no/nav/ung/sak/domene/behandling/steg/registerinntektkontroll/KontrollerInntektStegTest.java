package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.kontroll.KontrollerteInntektperioderTjeneste;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KontrollerInntektStegTest {

    private static final LocalDate FOM = LocalDate.of(2026, 8, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 8, 31);

    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private final KontrollerInntektInputMapper inputMapper = mock(KontrollerInntektInputMapper.class);
    private final RelevanteKontrollperioderUtleder kontrollPerioderUtleder = mock(RelevanteKontrollperioderUtleder.class);
    private final KontrollerteInntektperioderTjeneste kontrollerteInntektperioderTjeneste = mock(KontrollerteInntektperioderTjeneste.class);

    private KontrollerInntektSteg steg;
    private Behandling førstegangsbehandling;

    @BeforeEach
    void setUp() {
        var fagsak = new Fagsak(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), FOM.minusMonths(2), FOM.plusWeeks(52));
        førstegangsbehandling = Behandling.forFørstegangssøknad(fagsak).build();
        when(kontrollPerioderUtleder.utledPerioderForKontrollAvInntekt(any())).thenReturn(new LocalDateTimeline<>(FOM, TOM, true));
        when(inputMapper.mapInput(any())).thenReturn(new KontrollerInntektInput(
            LocalDateTimeline.empty(), LocalDateTimeline.empty(), LocalDateTimeline.empty(), LocalDateTimeline.empty()));
        steg = new KontrollerInntektSteg(kontrollerteInntektperioderTjeneste, behandlingRepository, inputMapper, kontrollPerioderUtleder, 15);
    }

    @Test
    void skal_alltid_opprette_aksjonspunkt_ved_manuelt_opprettet_revurdering_av_kontroll() {
        // Arrange
        var revurdering = lagRevurdering(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, true);

        // Act
        var resultat = steg.utførSteg(kontekst(revurdering));

        // Assert
        assertThat(resultat.getAksjonspunktListe()).containsExactly(AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
        verify(inputMapper, never()).mapInput(any());
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_uten_avvik_ved_automatisk_opprettet_revurdering_av_kontroll() {
        // Arrange
        var revurdering = lagRevurdering(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT, false);

        // Act
        var resultat = steg.utførSteg(kontekst(revurdering));

        // Assert
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        verify(inputMapper).mapInput(any());
    }

    @Test
    void skal_ikke_opprette_aksjonspunkt_uten_avvik_ved_annen_manuelt_opprettet_årsak() {
        // Arrange
        var revurdering = lagRevurdering(BehandlingÅrsakType.RE_SATS_ENDRING, true);

        // Act
        var resultat = steg.utførSteg(kontekst(revurdering));

        // Assert
        assertThat(resultat.getAksjonspunktListe()).isEmpty();
        verify(inputMapper).mapInput(any());
    }

    private Behandling lagRevurdering(BehandlingÅrsakType årsak, boolean manueltOpprettet) {
        var revurdering = Behandling.fraTidligereBehandling(førstegangsbehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(årsak).medManueltOpprettet(manueltOpprettet))
            .build();
        when(behandlingRepository.hentBehandling(ArgumentMatchers.<Long>any())).thenReturn(revurdering);
        return revurdering;
    }

    private static BehandlingskontrollKontekst kontekst(Behandling behandling) {
        return new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), new BehandlingLås(behandling.getId()));
    }
}
