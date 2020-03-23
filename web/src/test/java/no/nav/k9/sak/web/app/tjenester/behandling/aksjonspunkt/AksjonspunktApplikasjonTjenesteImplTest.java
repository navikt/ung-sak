package no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Collections;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakStatus;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.økonomi.tilbakekreving.TilbakekrevingVidereBehandling;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.kontrakt.søknad.AvklarSaksopplysningerDto;
import no.nav.k9.sak.kontrakt.vedtak.FatterVedtakAksjonspunktDto;
import no.nav.k9.sak.kontrakt.økonomi.tilbakekreving.VurderFeilutbetalingDto;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class AksjonspunktApplikasjonTjenesteImplTest {

    private static final String BEGRUNNELSE = "begrunnelse";

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjeneste;

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private FagsakRepository fagsakRepository;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    private AksjonspunktTestSupport aksjonspunktRepository = new AksjonspunktTestSupport();

    private AbstractTestScenario<?> lagScenarioMedAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(aksjonspunktDefinisjon, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
        return scenario;
    }

    @Test
    public void skal_sette_aksjonspunkt_til_utført_og_lagre_behandling() {
        // Arrange
        var behandling = opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS);

        var dto = new AvklarSaksopplysningerDto(BEGRUNNELSE + "2", PersonstatusType.BOSA, true);
        
        // Act
        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto), behandling.getId());

        // Assert
        Behandling oppdatertBehandling = behandlingRepository.hentBehandling(behandling.getId());
        Assertions.assertThat(oppdatertBehandling.getAksjonspunkter()).first().matches(a -> a.erUtført());

    }

    @Test
    public void skal_ikke_sette_ansvarlig_saksbehandler_hvis_bekreftet_aksjonspunkt_er_fatter_vedtak() {
        // Arrange
        // Bruker BekreftTerminbekreftelseAksjonspunktDto som konkret case
        AksjonspunktApplikasjonTjeneste aksjonspunktApplikasjonTjenesteImpl = aksjonspunktApplikasjonTjeneste;
        AbstractTestScenario<?> scenario = lagScenarioMedAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
        Behandling behandling = scenario.lagre(repositoryProvider);
        Behandling behandlingSpy = spy(behandling);

        FatterVedtakAksjonspunktDto dto = new FatterVedtakAksjonspunktDto(BEGRUNNELSE, Collections.emptyList());

        // Act
        aksjonspunktApplikasjonTjenesteImpl.setAnsvarligSaksbehandler(singletonList(dto), behandlingSpy);

        // Assert
        verify(behandlingSpy, never()).setAnsvarligSaksbehandler(any());
    }

    @Test
    public void skal_sette_totrinn_når_revurdering_ap_medfører_endring_i_grunnlag() {
        // Arrange
        Behandling førstegangsbehandling = opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS);
        aksjonspunktRepository.setTilUtført(førstegangsbehandling.getAksjonspunkter().iterator().next(), BEGRUNNELSE);
        Behandling revurdering = opprettRevurderingsbehandlingMedAksjonspunkt(førstegangsbehandling, AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS);
        AvklarSaksopplysningerDto dto = new AvklarSaksopplysningerDto(BEGRUNNELSE, PersonstatusType.UTVA, true);

        // Act
        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto), revurdering.getId());

        // Assert
        Behandling oppdatertBehandling = behandlingRepository.hentBehandling(revurdering.getId());
        Aksjonspunkt aksjonspunkt = oppdatertBehandling.getAksjonspunkter().iterator().next();
        assertThat(aksjonspunkt.isToTrinnsBehandling()).isTrue();
    }

    @Test
    public void skal_sette_totrinn_når_revurdering_ap_har_endring_i_begrunnelse() {
        // Arrange
        Behandling førstegangsbehandling = opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS);
        var dto1 = new AvklarSaksopplysningerDto(BEGRUNNELSE, PersonstatusType.BOSA, true);
        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto1), førstegangsbehandling.getId());

        Behandling revurdering = opprettRevurderingsbehandlingMedAksjonspunkt(førstegangsbehandling, AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS);
        var dto2 = new AvklarSaksopplysningerDto(BEGRUNNELSE + "2", PersonstatusType.BOSA, true);

        // Act
        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto2), revurdering.getId());

        // Assert
        Behandling oppdatertBehandling = behandlingRepository.hentBehandling(revurdering.getId());
        Aksjonspunkt aksjonspunkt = oppdatertBehandling.getAksjonspunkter().iterator().next();
        assertThat(aksjonspunkt.isToTrinnsBehandling()).isTrue();
    }

    @Test
    public void skal_sette_totrinn_når_revurdering_ap_verken_har_endring_i_grunnlag_eller_begrunnelse_men_et_bekreftet_ap_i_førstegangsbehandling() {
        // Arrange
        Behandling førstegangsbehandling = opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS);
        var dto1 = new AvklarSaksopplysningerDto(BEGRUNNELSE, PersonstatusType.BOSA, true);
        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto1), førstegangsbehandling.getId());

        Behandling revurdering = opprettRevurderingsbehandlingMedAksjonspunkt(førstegangsbehandling, AksjonspunktDefinisjon.AVKLAR_FAKTA_FOR_PERSONSTATUS);
        var dto2 = new AvklarSaksopplysningerDto(BEGRUNNELSE, PersonstatusType.BOSA, true);

        // Act
        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto2), revurdering.getId());

        // Assert
        Behandling oppdatertBehandling = behandlingRepository.hentBehandling(revurdering.getId());
        Aksjonspunkt aksjonspunkt = oppdatertBehandling.getAksjonspunkter().iterator().next();
        assertThat(aksjonspunkt.isToTrinnsBehandling()).isTrue();
    }

    @Test
    public void skal_ikke_sette_totrinn_når_aksjonspunktet_mangler_skjermlenke_selv_om_det_har_endring_i_begrunnelse() {
        // Arrange
        Behandling førstegangsbehandling = opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
        var dto1 = new VurderFeilutbetalingDto(BEGRUNNELSE, TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD, null);
        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto1), førstegangsbehandling.getId());

        Behandling revurdering = opprettRevurderingsbehandlingMedAksjonspunkt(førstegangsbehandling, AksjonspunktDefinisjon.VURDER_FEILUTBETALING);
        var dto2 = new VurderFeilutbetalingDto(BEGRUNNELSE + "2", TilbakekrevingVidereBehandling.TILBAKEKREV_I_INFOTRYGD, null);

        // Act
        aksjonspunktApplikasjonTjeneste.bekreftAksjonspunkter(singletonList(dto2), revurdering.getId());

        // Assert
        Behandling oppdatertBehandling = behandlingRepository.hentBehandling(revurdering.getId());
        Aksjonspunkt aksjonspunkt = oppdatertBehandling.getAksjonspunkter().iterator().next();
        assertThat(aksjonspunkt.isToTrinnsBehandling()).isFalse();
    }

    private Behandling opprettFørstegangsbehandlingMedAksjonspunkt(AksjonspunktDefinisjon aksjonspunktDefinisjon) {
        var førstegangsscenario = TestScenarioBuilder.builderMedSøknad();
        førstegangsscenario.medSøknad().medMottattDato(LocalDate.now());
        førstegangsscenario.leggTilAksjonspunkt(aksjonspunktDefinisjon, BehandlingStegType.SØKERS_RELASJON_TIL_BARN);
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
