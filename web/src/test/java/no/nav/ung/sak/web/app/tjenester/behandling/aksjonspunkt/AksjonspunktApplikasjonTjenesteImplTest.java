package no.nav.ung.sak.web.app.tjenester.behandling.aksjonspunkt;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Collections;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.kontrakt.vedtak.FatterVedtakAksjonspunktDto;
import no.nav.ung.sak.kontrakt.vedtak.ForeslaVedtakAksjonspunktDto;
import no.nav.ung.sak.test.util.behandling.AbstractTestScenario;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class AksjonspunktApplikasjonTjenesteImplTest {

    private static final String BEGRUNNELSE = "begrunnelse";

    @Inject
    private AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjeneste;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    private AksjonspunktTestSupport aksjonspunktRepository = new AksjonspunktTestSupport();
    @Inject
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;

    private AbstractTestScenario<?> lagScenarioMedAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(aksjonspunktDefinisjon, BehandlingStegType.KONTROLLER_FAKTA);
        return scenario;
    }

    @Test
    public void skal_sette_aksjonspunkt_til_utført_og_lagre_behandling() {
        // Arrange
        var behandling = opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandling.getId());

        var dto = new ForeslaVedtakAksjonspunktDto(BEGRUNNELSE, null, null, false, emptySet(), false);

        // Act
        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto), behandling.getId(), kontekst);

        // Assert
        Behandling oppdatertBehandling = behandlingRepository.hentBehandling(behandling.getId());
        var aksjonspunkter = oppdatertBehandling.getAksjonspunkter();
        var foreslåVedtakAP = aksjonspunkter.stream().filter(a -> a.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.FORESLÅ_VEDTAK)).findFirst().get();
        Assertions.assertThat(foreslåVedtakAP.erUtført()).isTrue();

    }

    @Test
    public void skal_ikke_sette_ansvarlig_saksbehandler_hvis_bekreftet_aksjonspunkt_er_fatter_vedtak() {
        // Arrange
        // Bruker BekreftTerminbekreftelseAksjonspunktDto som konkret case
        AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjenesteImpl = aksjonspunktApplikasjonTjeneste;
        AbstractTestScenario<?> scenario = lagScenarioMedAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK);
        Behandling behandling = scenario.lagre(repositoryProvider);
        Behandling behandlingSpy = spy(behandling);

        FatterVedtakAksjonspunktDto dto = new FatterVedtakAksjonspunktDto(BEGRUNNELSE, Collections.emptyList());

        // Act
        aksjonspunktApplikasjonTjenesteImpl.setAnsvarligSaksbehandler(singletonList(dto), behandlingSpy);

        // Assert
        verify(behandlingSpy, never()).setAnsvarligSaksbehandler(any());
    }

    // TODO: Vurder om disse testene skal tas inn når vi eventuelt tar i bruk totrinn
//
//    @Test
//    public void skal_sette_totrinn_når_revurdering_ap_medfører_endring_i_grunnlag() {
//        // Arrange
//        Behandling førstegangsbehandling = opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);
//        aksjonspunktRepository.setTilUtført(førstegangsbehandling.getAksjonspunkter().iterator().next(), BEGRUNNELSE);
//        Behandling revurdering = opprettRevurderingsbehandlingMedAksjonspunkt(førstegangsbehandling, AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);
//        var dto = new AvklarOpptjeningsvilkårDto(List.of(), List.of(), BEGRUNNELSE);
//        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering.getId());
//
//        // Act
//        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto), revurdering.getId(), kontekst);
//
//        // Assert
//        Behandling oppdatertBehandling = behandlingRepository.hentBehandling(revurdering.getId());
//        Aksjonspunkt aksjonspunkt = oppdatertBehandling.getAksjonspunkter().iterator().next();
//        assertThat(aksjonspunkt.isToTrinnsBehandling()).isTrue();
//    }
//
//    @Test
//    public void skal_sette_totrinn_når_revurdering_ap_har_endring_i_begrunnelse() {
//        // Arrange
//        Behandling førstegangsbehandling = opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);
//        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(førstegangsbehandling.getId());
//        var dto1 = new AvklarOpptjeningsvilkårDto(List.of(), List.of(), BEGRUNNELSE);
//        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto1), førstegangsbehandling.getId(), kontekst);
//
//        Behandling revurdering = opprettRevurderingsbehandlingMedAksjonspunkt(førstegangsbehandling, AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);
//        kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering.getId());
//        var dto2 = new AvklarOpptjeningsvilkårDto(List.of(), List.of(), BEGRUNNELSE);
//
//        // Act
//        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto2), revurdering.getId(), kontekst);
//
//        // Assert
//        Behandling oppdatertBehandling = behandlingRepository.hentBehandling(revurdering.getId());
//        Aksjonspunkt aksjonspunkt = oppdatertBehandling.getAksjonspunkter().iterator().next();
//        assertThat(aksjonspunkt.isToTrinnsBehandling()).isTrue();
//    }
//
//    @Test
//    public void skal_sette_totrinn_når_revurdering_ap_verken_har_endring_i_grunnlag_eller_begrunnelse_men_et_bekreftet_ap_i_førstegangsbehandling() {
//        // Arrange
//        Behandling førstegangsbehandling = opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);
//        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(førstegangsbehandling.getId());
//        var dto1 = new AvklarOpptjeningsvilkårDto(List.of(), List.of(), BEGRUNNELSE);
//        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto1), førstegangsbehandling.getId(), kontekst);
//
//        Behandling revurdering = opprettRevurderingsbehandlingMedAksjonspunkt(førstegangsbehandling, AksjonspunktDefinisjon.VURDER_OPPTJENINGSVILKÅRET);
//        kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering.getId());
//        var dto2 = new AvklarOpptjeningsvilkårDto(List.of(), List.of(), BEGRUNNELSE);
//
//        // Act
//        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto2), revurdering.getId(), kontekst);
//
//        // Assert
//        Behandling oppdatertBehandling = behandlingRepository.hentBehandling(revurdering.getId());
//        Aksjonspunkt aksjonspunkt = oppdatertBehandling.getAksjonspunkter().iterator().next();
//        assertThat(aksjonspunkt.isToTrinnsBehandling()).isTrue();
//    }
//
//    @Test
//    public void skal_ikke_sette_totrinn_når_aksjonspunktet_mangler_skjermlenke_selv_om_det_har_endring_i_begrunnelse() {
//        // Arrange
//        Behandling førstegangsbehandling = opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
//        var kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(førstegangsbehandling.getId());
//        var dto1 = new VurderFeilutbetalingDto(BEGRUNNELSE, TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, null);
//        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto1), førstegangsbehandling.getId(), kontekst);
//
//        Behandling revurdering = opprettRevurderingsbehandlingMedAksjonspunkt(førstegangsbehandling, AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
//        kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(revurdering.getId());
//        var dto2 = new VurderFeilutbetalingDto(BEGRUNNELSE + "2", TilbakekrevingVidereBehandling.OPPRETT_TILBAKEKREVING, null);
//
//        // Act
//        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto2), revurdering.getId(), kontekst);
//
//        // Assert
//        Behandling oppdatertBehandling = behandlingRepository.hentBehandling(revurdering.getId());
//        Aksjonspunkt aksjonspunkt = oppdatertBehandling.getAksjonspunkter().iterator().next();
//        assertThat(aksjonspunkt.isToTrinnsBehandling()).isFalse();
//    }

    private Behandling opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var førstegangsscenario = TestScenarioBuilder.builderMedSøknad();
        førstegangsscenario.medSøknad().medMottattDato(LocalDate.now());
        førstegangsscenario.leggTilAksjonspunkt(aksjonspunktDefinisjon, BehandlingStegType.FORESLÅ_VEDTAK);
        Behandling behandling = førstegangsscenario.lagre(repositoryProvider);
        return behandling;
    }

    private Behandling opprettRevurderingsbehandlingMedAksjonspunkt(Behandling førstegangsbehandling, AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        avsluttBehandlingOgFagsak(førstegangsbehandling);

        var revurderingsscenario = TestScenarioBuilder.builderMedSøknad()
            .medOriginalBehandling(førstegangsbehandling, BehandlingÅrsakType.RE_HENDELSE_FØDSEL)
            .medBehandlingType(BehandlingType.REVURDERING);
        revurderingsscenario.medSøknad().medMottattDato(LocalDate.now());
        revurderingsscenario.leggTilAksjonspunkt(aksjonspunktDefinisjon, BehandlingStegType.KONTROLLER_FAKTA);

        Behandling revurdering = revurderingsscenario.lagre(repositoryProvider);
        return revurdering;
    }

    private void avsluttBehandlingOgFagsak(Behandling behandling) {
        behandling.avsluttBehandling();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        fagsakRepository.oppdaterFagsakStatus(behandling.getFagsakId(), FagsakStatus.LØPENDE);
    }

}
