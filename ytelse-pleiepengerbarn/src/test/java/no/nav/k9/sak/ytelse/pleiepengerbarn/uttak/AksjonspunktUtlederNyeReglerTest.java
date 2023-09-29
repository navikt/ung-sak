package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.folketrygdloven.beregningsgrunnlag.tilkommetAktivitet.AktivitetstatusOgArbeidsgiver;
import no.nav.folketrygdloven.beregningsgrunnlag.tilkommetAktivitet.TilkommetAktivitetTjeneste;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.uttak.UttakNyeReglerRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.tjeneste.UttakTjeneste;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
public class AksjonspunktUtlederNyeReglerTest {


    @Inject
    private EntityManager entityManager;

    @Inject
    private AksjonspunktUtlederNyeRegler utleder;
    private UttakTjeneste uttakTjeneste = mock(UttakTjeneste.class);

    private TilkommetAktivitetTjeneste tilkommetAktivitetTjeneste = mock(TilkommetAktivitetTjeneste.class);

    private AksjonspunktRepository aksjonspunktRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;
    private Behandling førstegangsbehandling;
    private Behandling revurdering;

    private UttakNyeReglerRepository uttakNyeReglerRepository = mock(UttakNyeReglerRepository.class);
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;


    @BeforeEach
    void setUp() {
        behandlingRepository = new BehandlingRepository(entityManager);
        aksjonspunktKontrollRepository = new AksjonspunktKontrollRepository();
        utleder = new AksjonspunktUtlederNyeRegler(behandlingRepository, uttakTjeneste, uttakNyeReglerRepository, tilkommetAktivitetTjeneste, aksjonspunktKontrollRepository, true);


        aksjonspunktRepository = new AksjonspunktRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.DAGPENGER, new AktørId(123L), new Saksnummer("987"), LocalDate.now(), LocalDate.now());
        fagsakRepository.opprettNy(fagsak);
        førstegangsbehandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(førstegangsbehandling, behandlingRepository.taSkriveLås(førstegangsbehandling.getId()));

        revurdering = Behandling.fraTidligereBehandling(førstegangsbehandling, BehandlingType.REVURDERING).medBehandlingStatus(BehandlingStatus.UTREDES).build();

        behandlingRepository.lagre(revurdering, behandlingRepository.taSkriveLås(revurdering.getId()));

    }

    @Test
    void skal_få_aksjonspunkt_for_førstegangsbehandling_uten_dato_satt() {

        when(tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(anyLong(), any())).thenReturn(Map.of(new AktivitetstatusOgArbeidsgiver(UttakArbeidType.FRILANSER, null), LocalDateTimeline.empty()));
        when(uttakTjeneste.hentUttaksplan(førstegangsbehandling.getUuid(), false)).thenReturn(new Uttaksplan(Map.of(), List.of()));

        var aksjonspunktDefinisjon = utleder.utledAksjonspunktDatoForNyeRegler(førstegangsbehandling);

        assertThat(aksjonspunktDefinisjon).isPresent();
        assertThat(aksjonspunktDefinisjon.get()).isEqualTo(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
    }

    @Test
    void skal_få_aksjonspunkt_for_revurdering_uten_dato_satt() {

        when(tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(anyLong(), any())).thenReturn(Map.of(new AktivitetstatusOgArbeidsgiver(UttakArbeidType.FRILANSER, null), LocalDateTimeline.empty()));
        when(uttakTjeneste.hentUttaksplan(revurdering.getUuid(), false)).thenReturn(new Uttaksplan(Map.of(), List.of()));

        var aksjonspunktDefinisjon = utleder.utledAksjonspunktDatoForNyeRegler(revurdering);

        assertThat(aksjonspunktDefinisjon).isPresent();
        assertThat(aksjonspunktDefinisjon.get()).isEqualTo(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
    }


    @Test
    public void skal_få_ikke_få_aksjonspunkt_for_førstegangsbehandling_med_dato_satt_og_med_eksisterende_aksjonspunkt() {

        when(tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(anyLong(), any())).thenReturn(Map.of(new AktivitetstatusOgArbeidsgiver(UttakArbeidType.FRILANSER, null), LocalDateTimeline.empty()));
        when(uttakTjeneste.hentUttaksplan(førstegangsbehandling.getUuid(), false)).thenReturn(new Uttaksplan(Map.of(), List.of()));
        when(uttakNyeReglerRepository.finnDatoForNyeRegler(any())).thenReturn(Optional.of(LocalDate.now()));
        aksjonspunktKontrollRepository.leggTilAksjonspunkt(førstegangsbehandling, AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);


        var aksjonspunktDefinisjon = utleder.utledAksjonspunktDatoForNyeRegler(førstegangsbehandling);

        assertThat(aksjonspunktDefinisjon).isEmpty();
    }

    @Test
    void skal_få_ikke_få_aksjonspunkt_for_revurderig_med_dato_satt_og_med_eksisterende_aksjonspunkt() {

        when(tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(anyLong(), any())).thenReturn(Map.of(new AktivitetstatusOgArbeidsgiver(UttakArbeidType.FRILANSER, null), LocalDateTimeline.empty()));
        when(uttakTjeneste.hentUttaksplan(revurdering.getUuid(), false)).thenReturn(new Uttaksplan(Map.of(), List.of()));
        when(uttakNyeReglerRepository.finnDatoForNyeRegler(any())).thenReturn(Optional.of(LocalDate.now()));
        lagUtførtAksjonspunkt(førstegangsbehandling, "En fin begrunnelse", "VELDIG_ANSVARLIG_SAKSBEHANDLER");
        aksjonspunktKontrollRepository.leggTilAksjonspunkt(revurdering, AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
        lagUtførtAksjonspunkt(revurdering, "En enda finere begrunnelse", "VELDIG_UANSVARLIG_SAKSBEHANDLER");

        var aksjonspunktDefinisjon = utleder.utledAksjonspunktDatoForNyeRegler(revurdering);

        assertThat(aksjonspunktDefinisjon).isEmpty();


        var aksjonspunkter = revurdering.getAksjonspunkter();
        assertThat(aksjonspunkter.size()).isEqualTo(1);
        var revurderingAP = aksjonspunkter.iterator().next();
        assertThat(revurderingAP.getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
        assertThat(revurderingAP.getStatus()).isEqualTo(AksjonspunktStatus.UTFØRT);
        assertThat(revurderingAP.getBegrunnelse()).isEqualTo("En enda finere begrunnelse");
        assertThat(revurderingAP.getAnsvarligSaksbehandler()).isEqualTo("VELDIG_UANSVARLIG_SAKSBEHANDLER");
        assertThat(revurderingAP.isToTrinnsBehandling()).isFalse();
    }

    @Test
    void skal_få_utført_aksjonspunkt_for_revurdering_med_dato_satt_og_uten_eksisterende_aksjonspunkt() {

        when(tilkommetAktivitetTjeneste.finnTilkommedeAktiviteter(anyLong(), any())).thenReturn(Map.of(new AktivitetstatusOgArbeidsgiver(UttakArbeidType.FRILANSER, null), LocalDateTimeline.empty()));
        when(uttakTjeneste.hentUttaksplan(revurdering.getUuid(), false)).thenReturn(new Uttaksplan(Map.of(), List.of()));
        when(uttakNyeReglerRepository.finnDatoForNyeRegler(any())).thenReturn(Optional.of(LocalDate.now()));
        lagUtførtAksjonspunkt(førstegangsbehandling, "En fin begrunnelse", "VELDIG_ANSVARLIG_SAKSBEHANDLER");


        var aksjonspunktDefinisjon = utleder.utledAksjonspunktDatoForNyeRegler(revurdering);

        assertThat(aksjonspunktDefinisjon).isEmpty();

        var aksjonspunkter = revurdering.getAksjonspunkter();
        assertThat(aksjonspunkter.size()).isEqualTo(1);
        var revurderingAP = aksjonspunkter.iterator().next();
        assertThat(revurderingAP.getAksjonspunktDefinisjon()).isEqualTo(AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
        assertThat(revurderingAP.getStatus()).isEqualTo(AksjonspunktStatus.UTFØRT);
        assertThat(revurderingAP.getBegrunnelse()).isEqualTo("En fin begrunnelse");
        assertThat(revurderingAP.getAnsvarligSaksbehandler()).isEqualTo("VELDIG_ANSVARLIG_SAKSBEHANDLER");
        assertThat(revurderingAP.isToTrinnsBehandling()).isFalse();
    }

    private void lagUtførtAksjonspunkt(Behandling behandling, String begrunnelse, String ansvarligSaksbehandler) {
        var ap = aksjonspunktKontrollRepository.leggTilAksjonspunkt(behandling, AksjonspunktDefinisjon.VURDER_DATO_NY_REGEL_UTTAK);
        aksjonspunktKontrollRepository.setTilUtført(ap, begrunnelse);
        ap.setAnsvarligSaksbehandler(ansvarligSaksbehandler);
    }


}
