package no.nav.ung.sak.web.app.tjenester.behandling.vedtak;

import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.SkjermlenkeType;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.kontrakt.vedtak.TotrinnskontrollAksjonspunkterDto;
import no.nav.ung.sak.kontrakt.vedtak.TotrinnskontrollSkjermlenkeContextDto;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnresultatgrunnlag;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnsvurdering;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TotrinnskontrollAksjonspunkterTjenesteImplTest {

    private static final BehandlingStegType STEG_KONTROLLER_REGISTER_INNTEKT = BehandlingStegType.KONTROLLER_REGISTER_INNTEKT;
    private static final BehandlingStegType STEG_FATTE_VEDTAK = BehandlingStegType.FATTE_VEDTAK;

    private TotrinnskontrollAksjonspunkterTjeneste totrinnskontrollAksjonspunkterTjeneste;
    private TotrinnTjeneste totrinnTjeneste = Mockito.mock(TotrinnTjeneste.class);
    private TotrinnsaksjonspunktDtoTjeneste totrinnsaksjonspunktDtoTjeneste = Mockito.mock(TotrinnsaksjonspunktDtoTjeneste.class);
    private Behandling behandling;
    private Totrinnresultatgrunnlag totrinnresultatgrunnlag;

    private AksjonspunktTestSupport aksjonspunktRepository = new AksjonspunktTestSupport();

    private InternalManipulerBehandling manipulerInternBehandling = new InternalManipulerBehandling();

    @BeforeEach
    public void oppsett() {
        totrinnskontrollAksjonspunkterTjeneste = new TotrinnskontrollAksjonspunkterTjeneste(totrinnsaksjonspunktDtoTjeneste, totrinnTjeneste);
        totrinnresultatgrunnlag = new Totrinnresultatgrunnlag(behandling, null);
    }

    @Test
    public void skal_hente_tom_skjermlenkecontext_for_behandling_med_ikke_status_FATTER_VEDTAK_og_ingen_totrinnsvurdering_og_ingen_aksjonspunkter(){
        // Arrange
        opprettBehandlingFor(Optional.empty());
        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, STEG_KONTROLLER_REGISTER_INNTEKT);
        TotrinnskontrollAksjonspunkterDto totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.empty(), Optional.empty());
        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.emptyList());
        // Act
        List<TotrinnskontrollSkjermlenkeContextDto> context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling);
        // Assert
        assertThat(context).isEmpty();
    }

    @Test
    public void skal_hente_tom_skjermlenkecontext_for_behandling_med_status_FATTER_VEDTAK_og_ingen_totrinnsvurdering_og_ingen_aksjonspunkter(){
        // Arrange
        opprettBehandlingFor(Optional.empty());
        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, STEG_FATTE_VEDTAK);
        TotrinnskontrollAksjonspunkterDto totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.empty(), Optional.empty());
        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.emptyList());
        // Act
        List<TotrinnskontrollSkjermlenkeContextDto> context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling);
        // Assert
        assertThat(context).isEmpty();
    }

    @Test
    public void skal_hente_tom_skjermlenkecontext_for_behandling_med_ikke_status_FATTER_VEDTAK_og_med_totrinnsvurdering_og_ingen_aksjonspunkter(){

        // Arrange
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.KONTROLLER_INNTEKT;
        boolean ttvGodkjent = false;

        opprettBehandlingFor(Optional.empty());
        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, STEG_KONTROLLER_REGISTER_INNTEKT);

        TotrinnskontrollAksjonspunkterDto totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.empty(), Optional.empty());
        Totrinnsvurdering ttv = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon, ttvGodkjent);

        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttv));

        // Act
        List<TotrinnskontrollSkjermlenkeContextDto> context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling);
        // Assert
        assertThat(context).isEmpty();

    }

    @Test
    public void skal_hente_tom_skjermlenkecontext_for_behandling_med_status_FATTER_VEDTAK_og_med_totrinnsvurdering_og_ingen_aksjonspunkter(){
        // Arrange
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.KONTROLLER_INNTEKT;
        boolean ttvGodkjent = false;

        opprettBehandlingFor(Optional.empty());
        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, STEG_FATTE_VEDTAK);

        TotrinnskontrollAksjonspunkterDto totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.empty(), Optional.empty());
        Totrinnsvurdering ttv = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon, ttvGodkjent);

        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttv));

        // Act
        List<TotrinnskontrollSkjermlenkeContextDto> context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling);
        // Assert
        assertThat(context).isEmpty();
    }


    @Test
    public void skal_hente_en_skjermlenketype_og_ett_totrinnskontrollaksjonspunkt_for_behandling_med_en_totrinnsvurdering_og_ett_aksjonspunkt_som_omhandler_mottat_stotte(){

        // Arrange
        List<AksjonspunktDefinisjon> aksjonspunktDefinisjons = new ArrayList<>();
        aksjonspunktDefinisjons.add(AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
        boolean ttvGodkjent = false;
        boolean apAvbrutt = false;

        Map<VilkårType, SkjermlenkeType> vilkårTypeSkjermlenkeTypeMap = new HashMap<>();

        for (AksjonspunktDefinisjon aksjonspunktDefinisjon : aksjonspunktDefinisjons) {
            vilkårTypeSkjermlenkeTypeMap.keySet().forEach(vilkårType -> {

                opprettBehandlingFor(Optional.of(vilkårType));

                Totrinnsvurdering ttv = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon, ttvGodkjent);
                TotrinnskontrollAksjonspunkterDto totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(aksjonspunktDefinisjon), Optional.of(ttv));
                opprettAksjonspunkt(behandling, aksjonspunktDefinisjon, apAvbrutt);

                setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttv));

                // Act
                List<TotrinnskontrollSkjermlenkeContextDto> context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling);

                // Arrange
                assertThat(context).hasSize(1);

                TotrinnskontrollSkjermlenkeContextDto totrinnskontrollSkjermlenkeContextDto = context.get(0);
                assertThat(totrinnskontrollSkjermlenkeContextDto.getSkjermlenkeType()).isEqualTo(vilkårTypeSkjermlenkeTypeMap.get(vilkårType).getKode());

                List<TotrinnskontrollAksjonspunkterDto> totrinnskontrollAksjonspunkter = totrinnskontrollSkjermlenkeContextDto.getTotrinnskontrollAksjonspunkter();
                assertThat(totrinnskontrollAksjonspunkter).hasSize(1);

                TotrinnskontrollAksjonspunkterDto enesteTotrinnskontrollAksjonspunkt = totrinnskontrollAksjonspunkter.get(0);
                assertThat(enesteTotrinnskontrollAksjonspunkt.getAksjonspunktKode()).isEqualTo(aksjonspunktDefinisjon);
                assertThat(enesteTotrinnskontrollAksjonspunkt.getTotrinnskontrollGodkjent()).isFalse();

            });
        }

    }

    @Test
    public void skal_hente_en_skjermlenketype_og_ett_totrinnskontrollaksjonspunkt_for_behandling_med_status_FATTE_VEDTAK_og_ingen_totrinnsvurdering_og_ett_aksjonspunkt(){

        // Arrange
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.KONTROLLER_INNTEKT;
        boolean apAvbrutt = false;

        opprettBehandlingFor(Optional.empty());

        TotrinnskontrollAksjonspunkterDto totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(aksjonspunktDefinisjon), Optional.empty());
        opprettAksjonspunkt(behandling, aksjonspunktDefinisjon, apAvbrutt);

        manipulerInternBehandling.forceOppdaterBehandlingSteg(behandling, STEG_FATTE_VEDTAK);

        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.emptyList());

        // Act
        List<TotrinnskontrollSkjermlenkeContextDto> context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling);

        // Assert
        assertThat(context).hasSize(1);
        assertThat(context.get(0).getSkjermlenkeType()).isEqualTo(SkjermlenkeType.BEREGNING.getKode());
        List<TotrinnskontrollAksjonspunkterDto> totrinnskontrollAksjonspunkter = context.get(0).getTotrinnskontrollAksjonspunkter();
        assertThat(totrinnskontrollAksjonspunkter).hasSize(1);
        assertThat(totrinnskontrollAksjonspunkter.get(0).getAksjonspunktKode()).isEqualTo(AksjonspunktDefinisjon.KONTROLLER_INNTEKT);

    }

    @Test
    public void skal_hente_en_skjermlenketype_og_ett_ikke_godkjent_totrinnskontrollaksjonspunkt_for_behandling_med_en_godkjent_totrinnsvurdering_og_ett_aksjonspunkt_som_ikke_har_samme_aksjonspunktdefinisjon(){

        AksjonspunktDefinisjon adFraAksjonspunkt = AksjonspunktDefinisjon.KONTROLLER_INNTEKT;
        AksjonspunktDefinisjon adFraTotrinnvurdering = AksjonspunktDefinisjon.VURDER_FEILUTBETALING;
        boolean ttvGodkjent = true;
        boolean apAvbrutt = false;

        opprettBehandlingFor(Optional.empty());

        Totrinnsvurdering ttvFraBehandling = opprettTotrinnsvurdering(behandling, adFraTotrinnvurdering, ttvGodkjent);
        Totrinnsvurdering ttvOpprettetAvMetode = opprettTotrinnsvurdering(behandling, adFraAksjonspunkt, !ttvGodkjent);
        TotrinnskontrollAksjonspunkterDto totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(adFraAksjonspunkt), Optional.of(ttvOpprettetAvMetode));
        opprettAksjonspunkt(behandling, adFraAksjonspunkt, apAvbrutt);

        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttvFraBehandling));

        // Act
        List<TotrinnskontrollSkjermlenkeContextDto> context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling);

        // Assert
        assertThat(context).hasSize(1);
        assertThat(context.get(0).getSkjermlenkeType()).isEqualTo(SkjermlenkeType.BEREGNING.getKode());

        List<TotrinnskontrollAksjonspunkterDto> totrinnskontrollAksjonspunkter = context.get(0).getTotrinnskontrollAksjonspunkter();
        assertThat(totrinnskontrollAksjonspunkter).hasSize(1);

        TotrinnskontrollAksjonspunkterDto enesteTotrinnskontrollAksjonspunkt = totrinnskontrollAksjonspunkter.get(0);
        assertThat(enesteTotrinnskontrollAksjonspunkt.getAksjonspunktKode()).isEqualTo(adFraAksjonspunkt);
        assertThat(enesteTotrinnskontrollAksjonspunkt.getTotrinnskontrollGodkjent()).isFalse();

    }

    @Test
    public void skal_hente_en_tom_skjermlenkecontext_for_behandling_med_en_totrinnsvurdering_og_ett_avbrutt_aksjonspunkt(){

        // Arrange
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.KONTROLLER_INNTEKT;
        boolean ttvGodkjent = false;
        boolean apAvbrutt = true;

        opprettBehandlingFor(Optional.empty());

        Totrinnsvurdering ttv = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon, ttvGodkjent);
        TotrinnskontrollAksjonspunkterDto totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(aksjonspunktDefinisjon), Optional.of(ttv));
        opprettAksjonspunkt(behandling, aksjonspunktDefinisjon, apAvbrutt);

        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttv));

        // Act
        List<TotrinnskontrollSkjermlenkeContextDto> context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsSkjermlenkeContext(behandling);

        // Assert
        assertThat(context).isEmpty();

    }

    @Test
    public void skal_hente_en_tom_skjermlenkecontext_for_en_behandling_med_en_totrinnsvurdering_med_et_aksjonspunktdefinisjon_som_gir_en_undefinert_skjermlenketype(){

        // Arrange
        AksjonspunktDefinisjon aksjonspunktDefinisjon = AksjonspunktDefinisjon.VURDER_FEILUTBETALING;
        boolean ttvGodkjent = false;

        opprettBehandlingFor(Optional.empty());

        Totrinnsvurdering ttv = opprettTotrinnsvurdering(behandling, aksjonspunktDefinisjon, ttvGodkjent);
        TotrinnskontrollAksjonspunkterDto totrinnskontrollAksjonspunkterDto = opprettTotrinnskontrollAksjonspunkterDto(Optional.of(aksjonspunktDefinisjon), Optional.of(ttv));

        setFelleseMockMetoder(totrinnskontrollAksjonspunkterDto, Collections.singletonList(ttv));

        // Act
        List<TotrinnskontrollSkjermlenkeContextDto> context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsvurderingSkjermlenkeContext(behandling);

        // Assert
        assertThat(context).isEmpty();

    }

    @Test
    public void skal_hente_en_tom_skjermlenkecontext_for_en_behandling_med_ingen_totrinnaksjonspunktvurdering(){
        // Arrange
        opprettBehandlingFor(Optional.empty());
        when(totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling)).thenReturn(Collections.emptyList());
        // Act
        List<TotrinnskontrollSkjermlenkeContextDto> context = totrinnskontrollAksjonspunkterTjeneste.hentTotrinnsvurderingSkjermlenkeContext(behandling);
        // Assert
        assertThat(context).isEmpty();
    }

    private void opprettBehandlingFor(Optional<VilkårType> vilkårTypeOpt) {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        vilkårTypeOpt.ifPresent(vt -> scenario.leggTilVilkår(vt, Utfall.UDEFINERT));
        behandling = scenario.lagMocked();
    }

    private void setFelleseMockMetoder(TotrinnskontrollAksjonspunkterDto totrinnskontrollAksjonspunkterDto, List<Totrinnsvurdering> ttv) {
        when(totrinnTjeneste.hentTotrinnaksjonspunktvurderinger(behandling)).thenReturn(ttv);
        when(totrinnTjeneste.hentTotrinngrunnlagHvisEksisterer(behandling)).thenReturn(Optional.of(totrinnresultatgrunnlag));
        when(totrinnsaksjonspunktDtoTjeneste.lagTotrinnskontrollAksjonspunktDto(any()))
            .thenReturn(totrinnskontrollAksjonspunkterDto);
    }

    private TotrinnskontrollAksjonspunkterDto opprettTotrinnskontrollAksjonspunkterDto(Optional<AksjonspunktDefinisjon> aksjonspunktDefinisjonOpt, Optional<Totrinnsvurdering> ttvOpt) {
        TotrinnskontrollAksjonspunkterDto.Builder builder = new TotrinnskontrollAksjonspunkterDto.Builder();
        aksjonspunktDefinisjonOpt.ifPresent(ad -> builder.medAksjonspunktKode(ad));
        ttvOpt.ifPresent(ttv -> builder.medTotrinnskontrollGodkjent(ttv.isGodkjent()));
        return  builder.build();
    }

    private Totrinnsvurdering opprettTotrinnsvurdering(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, boolean godkjent) {
        return new Totrinnsvurdering.Builder(behandling, aksjonspunktDefinisjon)
            .medGodkjent(godkjent)
            .build();
    }

    private void opprettAksjonspunkt(Behandling behandling, AksjonspunktDefinisjon aksjonspunktDefinisjon, boolean erAvbrutt) {
        Aksjonspunkt aksjonspunkt = aksjonspunktRepository.leggTilAksjonspunkt(behandling, aksjonspunktDefinisjon);
        aksjonspunktRepository.setToTrinnsBehandlingKreves(aksjonspunkt);
        if (erAvbrutt) {
            aksjonspunktRepository.setTilAvbrutt(aksjonspunkt);
        } else {
            aksjonspunktRepository.setTilUtført(aksjonspunkt, "");
        }
    }

}
