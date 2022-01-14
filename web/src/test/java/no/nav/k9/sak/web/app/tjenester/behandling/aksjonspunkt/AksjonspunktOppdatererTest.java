package no.nav.k9.sak.web.app.tjenester.behandling.aksjonspunkt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.formidling.kontrakt.informasjonsbehov.InformasjonsbehovDto;
import no.nav.k9.formidling.kontrakt.informasjonsbehov.InformasjonsbehovListeDto;
import no.nav.k9.formidling.kontrakt.kodeverk.informasjonsbehov.InformasjonsbehovDatatype;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.k9.sak.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vedtak.VedtakVarselRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.behandling.steg.vurdermanueltbrev.K9FormidlingKlient;
import no.nav.k9.sak.domene.vedtak.VedtakTjeneste;
import no.nav.k9.sak.domene.vedtak.impl.FatterVedtakAksjonspunkt;
import no.nav.k9.sak.historikk.HistorikkTjenesteAdapter;
import no.nav.k9.sak.kontrakt.vedtak.AksjonspunktGodkjenningDto;
import no.nav.k9.sak.kontrakt.vedtak.FatterVedtakAksjonspunktDto;
import no.nav.k9.sak.kontrakt.vedtak.ForeslaVedtakAksjonspunktDto;
import no.nav.k9.sak.produksjonsstyring.totrinn.TotrinnRepository;
import no.nav.k9.sak.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.k9.sak.produksjonsstyring.totrinn.VurderÅrsakTotrinnsvurdering;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt.FatterVedtakAksjonspunktOppdaterer;
import no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt.ForeslåVedtakAksjonspunktOppdaterer;
import no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt.OpprettToTrinnsgrunnlag;
import no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt.VedtaksbrevHåndterer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class AksjonspunktOppdatererTest {

    @Inject
    public EntityManager entityManager;

    private K9FormidlingKlient formidlingKlient;

    private BehandlingRepositoryProvider repositoryProvider;

    @Inject
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;

    @Inject
    private TotrinnRepository totrinnRepository;

    @Inject
    private FatterVedtakAksjonspunkt fatterVedtakAksjonspunkt;

    @Inject
    private VedtakTjeneste vedtakTjeneste;

    @Inject
    private OpprettToTrinnsgrunnlag opprettTotrinnsgrunnlag;

    @Inject
    private VedtakVarselRepository vedtakVarselRepository;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        formidlingKlient = mock(K9FormidlingKlient.class);
        when(formidlingKlient.hentInformasjonsbehov(any(UUID.class), any(FagsakYtelseType.class))).thenReturn(mockTomtInformasjonsbehov());
    }

    @Test
    public void foreslå_vedtak_aksjonspunkt_setter_totrinn_på_pleiepenger_hvis_det_er_manuelt_brev() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad();

        Behandling behandling = scenario.lagre(repositoryProvider);

        aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.FORESLÅ_VEDTAK);

        BehandlingLås behandlingLås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling.getId());
        repositoryProvider.getBehandlingRepository().lagre(behandling, behandlingLås);

        var dto = new ForeslaVedtakAksjonspunktDto("begrunnelse", null, null, false, null, false);
        var vedtaksbrevHåndterer = new VedtaksbrevHåndterer(
            vedtakVarselRepository,
            mock(HistorikkTjenesteAdapter.class),
            opprettTotrinnsgrunnlag,
            vedtakTjeneste) {
            @Override
            protected String getCurrentUserId() {
                // return test verdi
                return "hello";
            }
        };

        var foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(vedtaksbrevHåndterer, formidlingKlient);

        when(formidlingKlient.hentInformasjonsbehov(any(UUID.class), any(FagsakYtelseType.class))).thenReturn(mockInformasjonsbehovMedKode());

        OppdateringResultat oppdateringResultat = foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        assertThat(behandling.getFagsakYtelseType()).isEqualTo(FagsakYtelseType.PSB);
        assertThat(oppdateringResultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    public void foreslå_vedtak_aksjonspunkt_setter_totrinn_på_pleiepenger_hvis_det_er_overstyrt_brev() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad();

        Behandling behandling = scenario.lagre(repositoryProvider);

        aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.FORESLÅ_VEDTAK);

        BehandlingLås behandlingLås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling.getId());
        repositoryProvider.getBehandlingRepository().lagre(behandling, behandlingLås);

        var dto = new ForeslaVedtakAksjonspunktDto("begrunnelse", null, null, true, null, false);
        var vedtaksbrevHåndterer = new VedtaksbrevHåndterer(
            vedtakVarselRepository,
            mock(HistorikkTjenesteAdapter.class),
            opprettTotrinnsgrunnlag,
            vedtakTjeneste) {
            @Override
            protected String getCurrentUserId() {
                // return test verdi
                return "hello";
            }
        };

        var foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(vedtaksbrevHåndterer, formidlingKlient);
        OppdateringResultat oppdateringResultat = foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));

        assertThat(behandling.getFagsakYtelseType()).isEqualTo(FagsakYtelseType.PSB);
        assertThat(oppdateringResultat.kreverTotrinnsKontroll()).isTrue();
    }

    @Test
    public void bekreft_foreslå_vedtak_aksjonspkt_setter_ansvarlig_saksbehandler() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad();

        Behandling behandling = scenario.lagre(repositoryProvider);

        var dto = new ForeslaVedtakAksjonspunktDto("begrunnelse", null, null, false, null, false);
        var vedtaksbrevHåndterer = new VedtaksbrevHåndterer(
            vedtakVarselRepository,
            mock(HistorikkTjenesteAdapter.class),
            opprettTotrinnsgrunnlag,
            vedtakTjeneste) {
            @Override
            protected String getCurrentUserId() {
                // return test verdi
                return "hello";
            }
        };

        var foreslaVedtakAksjonspunktOppdaterer = new ForeslåVedtakAksjonspunktOppdaterer(vedtaksbrevHåndterer, formidlingKlient);

        foreslaVedtakAksjonspunktOppdaterer.oppdater(dto, new AksjonspunktOppdaterParameter(behandling, Optional.empty(), dto));
        assertThat(behandling.getAnsvarligSaksbehandler()).isEqualTo("hello");
    }

    @Test
    public void oppdaterer_aksjonspunkt_med_beslutters_vurdering_ved_totrinnskontroll() {

        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD, BehandlingStegType.KONTROLLER_FAKTA);
        Behandling behandling = scenario.lagre(repositoryProvider);

        var aksGodkjDto = new AksjonspunktGodkjenningDto();
        aksGodkjDto.setArsaker(Set.of(VurderÅrsak.FEIL_FAKTA));
        aksGodkjDto.setGodkjent(false);
        String besluttersBegrunnelse = "Må ha bedre dokumentasjon.";
        aksGodkjDto.setBegrunnelse(besluttersBegrunnelse);
        aksGodkjDto.setAksjonspunktKode(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        var aksjonspunktDto = new FatterVedtakAksjonspunktDto("", Collections.singletonList(aksGodkjDto));
        new FatterVedtakAksjonspunktOppdaterer(fatterVedtakAksjonspunkt).oppdater(aksjonspunktDto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), aksjonspunktDto));

        Collection<Totrinnsvurdering> totrinnsvurderinger = totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling);
        assertThat(totrinnsvurderinger).hasSize(1);
        Totrinnsvurdering totrinnsvurdering = totrinnsvurderinger.iterator().next();

        assertThat(totrinnsvurdering.isGodkjent()).isFalse();
        assertThat(totrinnsvurdering.getBegrunnelse()).isEqualTo(besluttersBegrunnelse);
        assertThat(totrinnsvurdering.getVurderPåNyttÅrsaker()).hasSize(1);
        VurderÅrsakTotrinnsvurdering vurderPåNyttÅrsak = totrinnsvurdering.getVurderPåNyttÅrsaker().iterator().next();
        assertThat(vurderPåNyttÅrsak.getÅrsaksType()).isEqualTo(VurderÅrsak.FEIL_FAKTA);
    }

    @Test
    public void oppdaterer_aksjonspunkt_med_godkjent_totrinnskontroll() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD, BehandlingStegType.KONTROLLER_FAKTA);
        Behandling behandling = scenario.lagre(repositoryProvider);

        var aksGodkjDto = new AksjonspunktGodkjenningDto();
        aksGodkjDto.setGodkjent(true);
        aksGodkjDto.setAksjonspunktKode(AksjonspunktDefinisjon.VURDER_ARBEIDSFORHOLD);

        var aksjonspunktDto = new FatterVedtakAksjonspunktDto("", Collections.singletonList(aksGodkjDto));
        new FatterVedtakAksjonspunktOppdaterer(fatterVedtakAksjonspunkt).oppdater(aksjonspunktDto,
            new AksjonspunktOppdaterParameter(behandling, Optional.empty(), aksjonspunktDto));

        Collection<Totrinnsvurdering> totrinnsvurderinger = totrinnRepository.hentTotrinnaksjonspunktvurderinger(behandling);
        assertThat(totrinnsvurderinger).hasSize(1);
        Totrinnsvurdering totrinnsvurdering = totrinnsvurderinger.iterator().next();

        assertThat(totrinnsvurdering.isGodkjent()).isTrue();
        assertThat(totrinnsvurdering.getBegrunnelse()).isNullOrEmpty();
        assertThat(totrinnsvurdering.getVurderPåNyttÅrsaker()).isEmpty();
    }

    private InformasjonsbehovListeDto mockTomtInformasjonsbehov() {
        InformasjonsbehovListeDto dto = new InformasjonsbehovListeDto();
        dto.setInformasjonsbehov(Collections.emptyList());
        dto.setMangler(Collections.emptyList());
        return dto;
    }

    private InformasjonsbehovListeDto mockInformasjonsbehovMedKode() {
        InformasjonsbehovListeDto dto = new InformasjonsbehovListeDto();

        InformasjonsbehovDto informasjonsbehov = new InformasjonsbehovDto();
        informasjonsbehov.setKode("BEKREFTELSE");
        informasjonsbehov.setBeskrivelse("Dette er en test");
        informasjonsbehov.setType(InformasjonsbehovDatatype.FRITEKSTBREV);
        dto.setInformasjonsbehov(Arrays.asList(informasjonsbehov));
        dto.setMangler(Collections.emptyList());
        return dto;
    }

}
