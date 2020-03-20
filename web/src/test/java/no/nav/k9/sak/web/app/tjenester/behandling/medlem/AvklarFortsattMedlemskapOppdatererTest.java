package no.nav.k9.sak.web.app.tjenester.behandling.medlem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.medlem.AvklarFortsattMedlemskapDto;
import no.nav.k9.sak.kontrakt.medlem.BekreftedePerioderDto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.web.app.tjenester.behandling.medlem.AvklarFortsattMedlemskapOppdaterer;

public class AvklarFortsattMedlemskapOppdatererTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private LocalDate now = LocalDate.now();

    @Test
    public void avklar_fortsatt_medlemskap() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad()
            .medSøknadsdato(now);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_FORTSATT_MEDLEMSKAP, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);

        Behandling behandling = scenario.lagre(repositoryProvider);

        BekreftedePerioderDto bekreftetPeriode = new BekreftedePerioderDto();
        bekreftetPeriode.setVurderingsdato(now.plusDays(10));
        bekreftetPeriode.setAksjonspunkter(List.of(AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT.getKode()));

        AvklarFortsattMedlemskapDto dto = new AvklarFortsattMedlemskapDto("test", List.of(bekreftetPeriode));
        HistorikkTjenesteAdapter historikkTjenesteAdapter = mock(HistorikkTjenesteAdapter.class);
        HistorikkInnslagTekstBuilder historikkInnslagTekstBuilder = new HistorikkInnslagTekstBuilder();
        when(historikkTjenesteAdapter.tekstBuilder()).thenReturn(historikkInnslagTekstBuilder);

        final MedlemskapAksjonspunktTjeneste medlemskapTjeneste = new MedlemskapAksjonspunktTjeneste(
            repositoryProvider, historikkTjenesteAdapter, lagMockYtelseSkjæringstidspunktTjeneste(LocalDate.now()));
        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());

        // Act
        new AvklarFortsattMedlemskapOppdaterer(medlemskapTjeneste)
            .oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        Optional<VurdertMedlemskapPeriodeEntitet> vurdertMedlemskap = getVurdertLøpendeMedlemskap(behandling.getId(), repositoryProvider);
        assertThat(vurdertMedlemskap).isPresent();
    }

    private Optional<VurdertMedlemskapPeriodeEntitet> getVurdertLøpendeMedlemskap(Long behandlingId, BehandlingRepositoryProvider repositoryProvider) {
        MedlemskapRepository medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        return medlemskapRepository.hentVurdertLøpendeMedlemskap(behandlingId);
    }

    private SkjæringstidspunktTjeneste lagMockYtelseSkjæringstidspunktTjeneste(LocalDate fom){
        var skjæringstidspunktTjeneste = Mockito.mock(SkjæringstidspunktTjeneste.class);
        var skjæringstidspunkt = Skjæringstidspunkt.builder().medUtledetSkjæringstidspunkt(fom).build();
        when(skjæringstidspunktTjeneste.getSkjæringstidspunkter(Mockito.any())).thenReturn(skjæringstidspunkt);
        return skjæringstidspunktTjeneste;
    }
}
