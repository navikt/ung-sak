package no.nav.ung.sak.domene.behandling.steg.vedtak;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.ung.sak.behandlingskontroll.transisjoner.TransisjonIdentifikator;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class FatteVedtakTjenesteTest {

    @Inject
    private FatteVedtakTjeneste fatteVedtakTjeneste;

    @Inject
    private EntityManager entityManager;
    @Inject
    private BehandlingLåsRepository behandlingLåsRepository;
    @Inject
    private BehandlingVedtakRepository behandlingVedtakRepository;
    @Inject
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private TotrinnTjeneste totrinnTjeneste;

    @Test
    void skal_gå_videre_uten_aksjonspunkt_dersom_ingen_totrinn_på_behandling() {
        // Arrange
        var behandling = lagBehandlingUtenTotrinn();

        // Act
        var behandleStegResultat = fatteVedtakTjeneste.fattVedtak(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingLåsRepository.taLås(behandling.getId())), behandling);

        // Assert
        var aksjonspunktListe = behandleStegResultat.getAksjonspunktListe();

        assertThat(aksjonspunktListe.size()).isEqualTo(0);

        var behandlingVedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId());
        assertThat(behandlingVedtak.isPresent()).isTrue();

        var transisjon = behandleStegResultat.getTransisjon();
        assertThat(transisjon).isEqualTo(FellesTransisjoner.UTFØRT);
    }

    @Test
    void skal_utlede_fatte_vedtak_aksjonspunkt_for_behandling_med_totrinn_uten_aksjonspunkt() {
        // Arrange
        var behandling = lagTotrinnsbehandlingUtenFatteVedtak();

        // Act
        var behandleStegResultat = fatteVedtakTjeneste.fattVedtak(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingLåsRepository.taLås(behandling.getId())), behandling);

        // Assert
        var aksjonspunktListe = behandleStegResultat.getAksjonspunktListe();

        assertThat(aksjonspunktListe.size()).isEqualTo(1);
        assertThat(aksjonspunktListe.get(0)).isEqualTo(AksjonspunktDefinisjon.FATTER_VEDTAK);
    }


    @Test
    void skal_utlede_fatte_vedtak_aksjonspunkt_for_behandling_med_totrinn_med_avbrutt_aksjonspunkt() {
        // Arrange
        var behandling = lagTotrinnsbehandlingMedAvbruttFatteVedtak();

        // Act
        var behandleStegResultat = fatteVedtakTjeneste.fattVedtak(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingLåsRepository.taLås(behandling.getId())), behandling);

        // Assert
        var aksjonspunktListe = behandleStegResultat.getAksjonspunktListe();

        assertThat(aksjonspunktListe.size()).isEqualTo(1);
        assertThat(aksjonspunktListe.get(0)).isEqualTo(AksjonspunktDefinisjon.FATTER_VEDTAK);
    }

    @Test
    void skal_gå_videre_uten_aksjonspunkt_dersom_utført_med_godkjente_vurderinger() {
        // Arrange
        var behandling = lagTotrinnsbehandlingMedUtførtFatteVedtakOgGodkjentVurdering();

        // Act
        var behandleStegResultat = fatteVedtakTjeneste.fattVedtak(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingLåsRepository.taLås(behandling.getId())), behandling);

        // Assert
        var aksjonspunktListe = behandleStegResultat.getAksjonspunktListe();

        assertThat(aksjonspunktListe.size()).isEqualTo(0);

        var behandlingVedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId());
        assertThat(behandlingVedtak.isPresent()).isTrue();

        var transisjon = behandleStegResultat.getTransisjon();
        assertThat(transisjon).isEqualTo(FellesTransisjoner.UTFØRT);
    }

    @Test
    void skal_tilbakeføre_dersom_utført_med_ikke_godkjente_vurderinger() {
        // Arrange
        var behandling = lagTotrinnsbehandlingMedUtførtFatteVedtakUtenGodkjentVurdering();

        // Act
        var behandleStegResultat = fatteVedtakTjeneste.fattVedtak(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingLåsRepository.taLås(behandling.getId())), behandling);

        // Assert
        var aksjonspunktListe = behandleStegResultat.getAksjonspunktListe();
        assertThat(aksjonspunktListe.size()).isEqualTo(1);
        assertThat(aksjonspunktListe.get(0)).isEqualTo(AksjonspunktDefinisjon.KONTROLLER_INNTEKT);

        var behandlingVedtak = behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandling.getId());
        assertThat(behandlingVedtak.isPresent()).isFalse();

        var transisjon = behandleStegResultat.getTransisjon();
        assertThat(transisjon).isEqualTo(FellesTransisjoner.TILBAKEFØRT_TIL_AKSJONSPUNKT);
    }


    private Behandling lagTotrinnsbehandlingUtenFatteVedtak() {
        var testScenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        testScenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);
        testScenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK, BehandlingStegType.FORESLÅ_VEDTAK);
        testScenarioBuilder.medBehandlingsresultat(BehandlingResultatType.INNVILGET).medBehandlingStatus(BehandlingStatus.FATTER_VEDTAK);
        var behandling = testScenarioBuilder.lagre(entityManager);
        behandling.setToTrinnsBehandling();
        return behandling;
    }


    private Behandling lagTotrinnsbehandlingMedAvbruttFatteVedtak() {
        var testScenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        testScenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);
        testScenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK, BehandlingStegType.FORESLÅ_VEDTAK);
        testScenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.FATTER_VEDTAK, BehandlingStegType.FATTE_VEDTAK);
        testScenarioBuilder.medBehandlingsresultat(BehandlingResultatType.INNVILGET).medBehandlingStatus(BehandlingStatus.FATTER_VEDTAK);
        var behandling = testScenarioBuilder.lagre(entityManager);
        behandling.setToTrinnsBehandling();
        aksjonspunktKontrollRepository.setTilAvbrutt(behandling.getAksjonspunktFor(AksjonspunktDefinisjon.FATTER_VEDTAK));
        behandlingRepository.lagre(behandling, behandlingLåsRepository.taLås(behandling.getId()));
        return behandling;
    }

    private Behandling lagTotrinnsbehandlingMedUtførtFatteVedtakOgGodkjentVurdering() {
        var testScenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        testScenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);
        testScenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK, BehandlingStegType.FORESLÅ_VEDTAK);
        testScenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.FATTER_VEDTAK, BehandlingStegType.FATTE_VEDTAK);
        testScenarioBuilder.medBehandlingsresultat(BehandlingResultatType.INNVILGET).medBehandlingStatus(BehandlingStatus.FATTER_VEDTAK);
        var behandling = testScenarioBuilder.lagre(entityManager);
        behandling.setToTrinnsBehandling();
        aksjonspunktKontrollRepository.setTilUtført(behandling.getAksjonspunktFor(AksjonspunktDefinisjon.FATTER_VEDTAK), "begrunnelse");

        var totrinnvurderinBuilder = new Totrinnsvurdering.Builder(behandling, AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
        totrinnTjeneste.settNyeTotrinnaksjonspunktvurderinger(behandling, List.of(
            totrinnvurderinBuilder
                .medGodkjent(true)
                .medBegrunnelse("Godkjent")
                .build()
        ));

        behandlingRepository.lagre(behandling, behandlingLåsRepository.taLås(behandling.getId()));
        return behandling;
    }

        private Behandling lagTotrinnsbehandlingMedUtførtFatteVedtakUtenGodkjentVurdering() {
            var testScenarioBuilder = TestScenarioBuilder.builderMedSøknad();
            testScenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);
            testScenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.FORESLÅ_VEDTAK, BehandlingStegType.FORESLÅ_VEDTAK);
            testScenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.FATTER_VEDTAK, BehandlingStegType.FATTE_VEDTAK);
            testScenarioBuilder.medBehandlingsresultat(BehandlingResultatType.INNVILGET).medBehandlingStatus(BehandlingStatus.FATTER_VEDTAK);
            var behandling = testScenarioBuilder.lagre(entityManager);
            behandling.setToTrinnsBehandling();
            aksjonspunktKontrollRepository.setTilUtført(behandling.getAksjonspunktFor(AksjonspunktDefinisjon.FATTER_VEDTAK), "begrunnelse");

            var totrinnvurderinBuilder = new Totrinnsvurdering.Builder(behandling, AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
            totrinnTjeneste.settNyeTotrinnaksjonspunktvurderinger(behandling, List.of(
                totrinnvurderinBuilder
                    .medGodkjent(false)
                    .medVurderÅrsak(VurderÅrsak.FEIL_FAKTA)
                    .medBegrunnelse("Godkjent")
                    .build()
            ));

            behandlingRepository.lagre(behandling, behandlingLåsRepository.taLås(behandling.getId()));
            return behandling;
        }

    private Behandling lagBehandlingUtenTotrinn() {
        var testScenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        testScenarioBuilder.medBehandlingsresultat(BehandlingResultatType.INNVILGET).medBehandlingStatus(BehandlingStatus.FATTER_VEDTAK);
        var behandling = testScenarioBuilder.lagre(entityManager);
        return behandling;
    }
}
