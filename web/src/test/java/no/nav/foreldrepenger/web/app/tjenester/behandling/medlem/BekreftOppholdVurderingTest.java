package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem;

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

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.foreldrepenger.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.VurderMedlemskapTjeneste;
import no.nav.foreldrepenger.domene.medlem.impl.HentMedlemskapFraRegister;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.historikk.HistorikkInnslagTekstBuilder;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjenesteImpl;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.kontrakt.medlem.BekreftLovligOppholdVurderingDto;
import no.nav.k9.sak.kontrakt.medlem.BekreftOppholdsrettVurderingDto;
import no.nav.k9.sak.kontrakt.medlem.BekreftedePerioderDto;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class BekreftOppholdVurderingTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repositoryRule.getEntityManager());
    private final HistorikkInnslagTekstBuilder tekstBuilder = new HistorikkInnslagTekstBuilder();
    private PersonopplysningTjeneste personopplysningTjeneste = mock(PersonopplysningTjeneste.class);

    @Inject
    private SkjæringstidspunktTjenesteImpl skjæringstidspunktTjeneste;

    private LocalDate now = LocalDate.now();

    @Test
    public void bekreft_oppholdsrett_vurdering() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad()
            .medSøknadsdato(now);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);
        Behandling behandling = scenario.lagre(repositoryProvider);
        BekreftedePerioderDto bekreftetPeriode = new BekreftedePerioderDto();
        bekreftetPeriode.setOppholdsrettVurdering(true);
        bekreftetPeriode.setLovligOppholdVurdering(true);
        bekreftetPeriode.setErEosBorger(true);

        BekreftOppholdsrettVurderingDto dto = new BekreftOppholdsrettVurderingDto("test", List.of(bekreftetPeriode));

        // Act
        final MedlemTjeneste medlemskapTjeneste = new MedlemTjeneste(repositoryProvider,
            mock(HentMedlemskapFraRegister.class), skjæringstidspunktTjeneste,
            personopplysningTjeneste, mock(UtledVurderingsdatoerForMedlemskapTjeneste.class), mock(VurderMedlemskapTjeneste.class));
        final MedlemskapAksjonspunktTjeneste medlemskapAksjonspunktTjeneste = new MedlemskapAksjonspunktTjeneste(
            repositoryProvider, mock(HistorikkTjenesteAdapter.class), skjæringstidspunktTjeneste);

        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        BekreftOppholdOppdaterer bekreftOppholdOppdaterer = new BekreftOppholdOppdaterer(
            lagMockHistory(), medlemskapTjeneste, medlemskapAksjonspunktTjeneste) {
        };
        bekreftOppholdOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        VurdertMedlemskap vurdertMedlemskap = getVurdertMedlemskap(behandling.getId(), repositoryProvider);
        assertThat(vurdertMedlemskap.getOppholdsrettVurdering()).isTrue();
    }

    private VurdertMedlemskap getVurdertMedlemskap(Long behandlingId, BehandlingRepositoryProvider repositoryProvier) {
        MedlemskapRepository medlemskapRepository = repositoryProvier.getMedlemskapRepository();
        Optional<VurdertMedlemskap> vurdertMedlemskap = medlemskapRepository.hentVurdertMedlemskap(behandlingId);
        return vurdertMedlemskap.orElse(null);
    }

    @Test
    public void bekreft_lovlig_opphold_vurdering() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad()
            .medSøknadsdato(now);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD, BehandlingStegType.VURDER_MEDLEMSKAPVILKÅR);

        Behandling behandling = scenario.lagre(repositoryProvider);
        BekreftedePerioderDto bekreftetPeriode = new BekreftedePerioderDto();
        bekreftetPeriode.setOppholdsrettVurdering(true);
        bekreftetPeriode.setLovligOppholdVurdering(true);
        bekreftetPeriode.setErEosBorger(true);

        BekreftLovligOppholdVurderingDto dto = new BekreftLovligOppholdVurderingDto("test", List.of(bekreftetPeriode));

        // Act
        final MedlemTjeneste medlemskapTjeneste = new MedlemTjeneste(repositoryProvider,
            mock(HentMedlemskapFraRegister.class), skjæringstidspunktTjeneste,
            personopplysningTjeneste, mock(UtledVurderingsdatoerForMedlemskapTjeneste.class), mock(VurderMedlemskapTjeneste.class));
        final MedlemskapAksjonspunktTjeneste medlemskapAksjonspunktTjeneste = new MedlemskapAksjonspunktTjeneste(
            repositoryProvider, mock(HistorikkTjenesteAdapter.class), skjæringstidspunktTjeneste);

        var aksjonspunkt = behandling.getAksjonspunktFor(dto.getKode());
        new BekreftOppholdOppdaterer(lagMockHistory(), medlemskapTjeneste, medlemskapAksjonspunktTjeneste) {
        }.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        Long behandlingId = behandling.getId();
        VurdertMedlemskap vurdertMedlemskap = getVurdertMedlemskap(behandlingId, repositoryProvider);
        assertThat(vurdertMedlemskap.getLovligOppholdVurdering()).isTrue();
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }
}
