package no.nav.k9.sak.web.app.tjenester.behandling.medlem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.VurdertMedlemskapPeriodeEntitet;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.medlem.MedlemTjeneste;
import no.nav.k9.sak.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.k9.sak.domene.medlem.UtledVurderingsdatoerForMedlemskapTjeneste;
import no.nav.k9.sak.domene.medlem.VurderMedlemskapTjeneste;
import no.nav.k9.sak.domene.medlem.impl.HentMedlemskapFraRegister;
import no.nav.k9.sak.historikk.HistorikkInnslagTekstBuilder;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.medlem.BekreftLovligOppholdVurderingDto;
import no.nav.k9.sak.kontrakt.medlem.BekreftOppholdsrettVurderingDto;
import no.nav.k9.sak.kontrakt.medlem.BekreftedePerioderDto;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjenesteImpl;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BekreftOppholdVurderingTest {

    @Inject
    public EntityManager entityManager;

    @Inject
    private SkjæringstidspunktTjenesteImpl skjæringstidspunktTjeneste;

    private BehandlingRepositoryProvider repositoryProvider ;
    private HistorikkInnslagTekstBuilder tekstBuilder ;
    private LocalDate now = LocalDate.now();

    @BeforeEach
    public void setup(){
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        tekstBuilder = new HistorikkInnslagTekstBuilder();
        now = LocalDate.now();
    }

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
            mock(HentMedlemskapFraRegister.class),
            mock(UtledVurderingsdatoerForMedlemskapTjeneste.class), mock(VurderMedlemskapTjeneste.class));
        final MedlemskapAksjonspunktTjeneste medlemskapAksjonspunktTjeneste = new MedlemskapAksjonspunktTjeneste(
            repositoryProvider, mock(HistorikkTjenesteAdapter.class), skjæringstidspunktTjeneste);

        Optional<Aksjonspunkt> aksjonspunkt = behandling.getAksjonspunktForHvisFinnes(dto.getKode());
        BekreftOppholdOppdaterer bekreftOppholdOppdaterer = new BekreftOppholdOppdaterer(
            lagMockHistory(), medlemskapTjeneste, medlemskapAksjonspunktTjeneste) {
        };
        bekreftOppholdOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        var vurdertMedlemskap = getVurdertMedlemskap(behandling.getId(), repositoryProvider);
        assertThat(vurdertMedlemskap.getPerioder().iterator().next().getOppholdsrettVurdering()).isTrue();
    }

    private VurdertMedlemskapPeriodeEntitet getVurdertMedlemskap(Long behandlingId, BehandlingRepositoryProvider repositoryProvider) {
        MedlemskapRepository medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        Optional<VurdertMedlemskapPeriodeEntitet> vurdertMedlemskap = medlemskapRepository.hentVurdertLøpendeMedlemskap(behandlingId);
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
            mock(HentMedlemskapFraRegister.class),
            mock(UtledVurderingsdatoerForMedlemskapTjeneste.class), mock(VurderMedlemskapTjeneste.class));
        final MedlemskapAksjonspunktTjeneste medlemskapAksjonspunktTjeneste = new MedlemskapAksjonspunktTjeneste(
            repositoryProvider, mock(HistorikkTjenesteAdapter.class), skjæringstidspunktTjeneste);

        var aksjonspunkt = behandling.getAksjonspunktForHvisFinnes(dto.getKode());
        new BekreftOppholdOppdaterer(lagMockHistory(), medlemskapTjeneste, medlemskapAksjonspunktTjeneste) {
        }.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, aksjonspunkt, dto));

        // Assert
        Long behandlingId = behandling.getId();
        var vurdertMedlemskap = getVurdertMedlemskap(behandlingId, repositoryProvider);
        assertThat(vurdertMedlemskap.getPerioder().iterator().next().getLovligOppholdVurdering()).isTrue();
    }

    private HistorikkTjenesteAdapter lagMockHistory() {
        HistorikkTjenesteAdapter mockHistory = Mockito.mock(HistorikkTjenesteAdapter.class);
        Mockito.when(mockHistory.tekstBuilder()).thenReturn(tekstBuilder);
        return mockHistory;
    }
}
