package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;

class UtledVirkningsdatoNyeReglerTest {

    private static final long BEHANDLING_ID = 1L;
    private static final long ORIGINAL_BEHANDLING_ID = 2L;
    private VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = mock(VilkårsPerioderTilVurderingTjeneste.class);
    private UtledVirkningsdatoNyeUttaksregler utledVirkningsdatoNyeUttaksregler;
    private BehandlingReferanse behandlingReferanse = mock(BehandlingReferanse.class);

    @BeforeEach
    void setUp() {
        utledVirkningsdatoNyeUttaksregler = new UtledVirkningsdatoNyeUttaksregler(new UnitTestLookupInstanceImpl<>(vilkårsPerioderTilVurderingTjeneste), true);
        when(behandlingReferanse.getBehandlingId()).thenReturn(BEHANDLING_ID);
        when(behandlingReferanse.getBehandlingType()).thenReturn(BehandlingType.REVURDERING);
        when(behandlingReferanse.getFagsakYtelseType()).thenReturn(FagsakYtelseType.PLEIEPENGER_SYKT_BARN);
        when(behandlingReferanse.getOriginalBehandlingId()).thenReturn(Optional.of(ORIGINAL_BEHANDLING_ID));
    }

    @Test
    void skal_returnere_tomt_resultat_dersom_forrige_perioder_er_lik_gjeldende_perioder() {
        var perioder = new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now())));
        when(vilkårsPerioderTilVurderingTjeneste.utledFullstendigePerioder(eq(BEHANDLING_ID)))
            .thenReturn(perioder);
        when(vilkårsPerioderTilVurderingTjeneste.utledFullstendigePerioder(eq(ORIGINAL_BEHANDLING_ID)))
            .thenReturn(perioder);

        var førsteNyeDag = utledVirkningsdatoNyeUttaksregler.utledDato(behandlingReferanse);

        assertThat(førsteNyeDag.isPresent()).isFalse();
    }

    @Test
    void skal_returnere_dato_dersom_nye_perioder_er_en_dag_etter_forrige() {
        var perioder = new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(1))));
        var originalePerioder = new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now())));
        when(vilkårsPerioderTilVurderingTjeneste.utledFullstendigePerioder(eq(BEHANDLING_ID)))
            .thenReturn(perioder);
        when(vilkårsPerioderTilVurderingTjeneste.utledFullstendigePerioder(eq(ORIGINAL_BEHANDLING_ID)))
            .thenReturn(originalePerioder);

        var førsteNyeDag = utledVirkningsdatoNyeUttaksregler.utledDato(behandlingReferanse);

        assertThat(førsteNyeDag.isPresent()).isTrue();
        assertThat(førsteNyeDag.get()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    void skal_returnere_dato_dersom_nye_perioder_er_en_dag_før_forrige() {
        var perioder = new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(1), LocalDate.now())));
        var originalePerioder = new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now())));
        when(vilkårsPerioderTilVurderingTjeneste.utledFullstendigePerioder(eq(BEHANDLING_ID)))
            .thenReturn(perioder);
        when(vilkårsPerioderTilVurderingTjeneste.utledFullstendigePerioder(eq(ORIGINAL_BEHANDLING_ID)))
            .thenReturn(originalePerioder);

        var førsteNyeDag = utledVirkningsdatoNyeUttaksregler.utledDato(behandlingReferanse);

        assertThat(førsteNyeDag.isPresent()).isTrue();
        assertThat(førsteNyeDag.get()).isEqualTo(LocalDate.now().minusDays(1));
    }

    @Test
    void skal_returnere_tomt_resultat_dersom_det_er_trukket_en_dag() {
        var perioder = new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now())));
        var originalePerioder = new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(1), LocalDate.now())));
        when(vilkårsPerioderTilVurderingTjeneste.utledFullstendigePerioder(eq(BEHANDLING_ID)))
            .thenReturn(perioder);
        when(vilkårsPerioderTilVurderingTjeneste.utledFullstendigePerioder(eq(ORIGINAL_BEHANDLING_ID)))
            .thenReturn(originalePerioder);

        var førsteNyeDag = utledVirkningsdatoNyeUttaksregler.utledDato(behandlingReferanse);

        assertThat(førsteNyeDag.isPresent()).isFalse();
    }

    @Test
    void skal_returnere_første_dag_dersom_flere_perioder_med_diff() {
        var perioder = new TreeSet<>(Set.of(
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(1)),
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().plusDays(10), LocalDate.now().plusDays(12))));
        var originalePerioder = new TreeSet<>(Set.of(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(1), LocalDate.now())));
        when(vilkårsPerioderTilVurderingTjeneste.utledFullstendigePerioder(eq(BEHANDLING_ID)))
            .thenReturn(perioder);
        when(vilkårsPerioderTilVurderingTjeneste.utledFullstendigePerioder(eq(ORIGINAL_BEHANDLING_ID)))
            .thenReturn(originalePerioder);

        var førsteNyeDag = utledVirkningsdatoNyeUttaksregler.utledDato(behandlingReferanse);

        assertThat(førsteNyeDag.isPresent()).isTrue();
        assertThat(førsteNyeDag.get()).isEqualTo(LocalDate.now().plusDays(1));
    }

}
