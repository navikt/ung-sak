package no.nav.k9.sak.web.app.tjenester.behandling.medlem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.medlem.MedlemskapManuellVurderingType;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.medlem.BekreftErMedlemVurderingDto;
import no.nav.k9.sak.kontrakt.medlem.BekreftedePerioderDto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjenesteImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BekreftErMedlemOppdatererTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();

    @Inject
    private SkjæringstidspunktTjenesteImpl skjæringstidspunktTjeneste;

    private LocalDate now = LocalDate.now();

    @Test
    public void bekreft_er_medlem_vurdering() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad()
            .medSøknadsdato(now);

        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);

        Behandling behandling = scenario.lagre(repositoryProvider);
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
        BekreftedePerioderDto bekreftetPeriode = new BekreftedePerioderDto();
        bekreftetPeriode.setMedlemskapManuellVurderingType(MedlemskapManuellVurderingType.MEDLEM);

        BekreftErMedlemVurderingDto dto = new BekreftErMedlemVurderingDto("test", List.of(bekreftetPeriode));

        // Act
        final MedlemskapAksjonspunktTjeneste medlemskapTjeneste = new MedlemskapAksjonspunktTjeneste(
            repositoryProvider, mock(HistorikkTjenesteAdapter.class), skjæringstidspunktTjeneste);
        new BekreftErMedlemVurderingOppdaterer(repositoryProvider, lagMockHistory(), medlemskapTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        var vurdertMedlemskap = getVurdertMedlemskap(behandling.getId(), repositoryProvider);
        assertThat(vurdertMedlemskap.getPerioder().iterator().next().getMedlemsperiodeManuellVurdering())
            .isEqualTo(MedlemskapManuellVurderingType.MEDLEM);
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }

    private VurdertMedlemskapPeriodeEntitet getVurdertMedlemskap(Long behandlingId, BehandlingRepositoryProvider repositoryProvider) {
        MedlemskapRepository medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        Optional<VurdertMedlemskapPeriodeEntitet> vurdertMedlemskap = medlemskapRepository.hentVurdertLøpendeMedlemskap(behandlingId);
        return vurdertMedlemskap.orElse(null);
    }
}
