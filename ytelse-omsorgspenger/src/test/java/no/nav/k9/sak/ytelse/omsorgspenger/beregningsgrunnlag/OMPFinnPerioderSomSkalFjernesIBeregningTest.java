package no.nav.k9.sak.ytelse.omsorgspenger.beregningsgrunnlag;

import no.nav.k9.aarskvantum.kontrakter.*;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.DefaultKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OMPFinnPerioderSomSkalFjernesIBeregningTest {

    BehandlingReferanse behandlingReferanseMock = mock(BehandlingReferanse.class);
    VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = mock(VilkårsPerioderTilVurderingTjeneste.class);

    private ÅrskvantumTjeneste årskvantumTjenesteMock = mock(ÅrskvantumTjeneste.class);
    private OMPFinnPerioderSomSkalFjernesIBeregning finnPerioderSomSkalFjernesIBeregning = new OMPFinnPerioderSomSkalFjernesIBeregning(årskvantumTjenesteMock, new UnitTestLookupInstanceImpl<>(vilkårsPerioderTilVurderingTjeneste));

    private static final Saksnummer saksnummer = new Saksnummer("AAAAA");

    @BeforeEach
    void setup() {
        when(behandlingReferanseMock.getSaksnummer()).thenReturn(saksnummer);
        when(vilkårsPerioderTilVurderingTjeneste.getKantIKantVurderer()).thenReturn(new DefaultKantIKantVurderer());
    }

    @Test
    void fjern_perioder_hvor_det_er_avslåtte_perioder_i_uttaksplan() {
        var fom = LocalDate.of(2024, 2, 1);
        var tom = fom.plusDays(3);

        var fullUttaksplan = new FullUttaksplan(saksnummer.getVerdi(), List.of(
            new Aktivitet(
                mock(Arbeidsforhold.class),
                List.of(
                   avslått(fom, tom)
                )
            )
        ));

        when(årskvantumTjenesteMock.hentFullUttaksplan(saksnummer)).thenReturn(fullUttaksplan);

        var vilkårene = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fra(fom, tom)), List.of(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .build();

        var perioder = finnPerioderSomSkalFjernesIBeregning.finnPerioderSomSkalFjernes(vilkårene, behandlingReferanseMock);

        assertThat(perioder).isNotEmpty();
    }

    @Test
    void fjern_perioder_med_minst_ett_avslått_vilkår_i_beregning() {
        var fom = LocalDate.of(2024, 2, 1);
        var tom = fom.plusDays(3);

        var fullUttaksplan = new FullUttaksplan(saksnummer.getVerdi(), List.of());

        when(årskvantumTjenesteMock.hentFullUttaksplan(saksnummer)).thenReturn(fullUttaksplan);

        var avslåttVilkårBuilder = new VilkårBuilder(VilkårType.ALDERSVILKÅR)
            .leggTil( new VilkårPeriodeBuilder()
                .medPeriode(fom, tom)
                .medUtfall(no.nav.k9.kodeverk.vilkår.Utfall.IKKE_OPPFYLT)
            );

        var vilkårene = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fra(fom, tom)), List.of(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .leggTil(avslåttVilkårBuilder)
            .build();

        var perioderSomSkalFjernes = finnPerioderSomSkalFjernesIBeregning.finnPerioderSomSkalFjernes(vilkårene, behandlingReferanseMock);
        assertThat(perioderSomSkalFjernes).isNotEmpty();
    }

    @Test
    void ikke_fjern_perioder_hvor_det_ikke_er_avslåtte_perioder_i_uttaksplan() {
        var fom = LocalDate.of(2024, 2, 1);
        var tom = fom.plusDays(3);

        var fullUttaksplan = new FullUttaksplan(saksnummer.getVerdi(), List.of(
            new Aktivitet(
                mock(Arbeidsforhold.class),
                List.of()
            )
        ));

        when(årskvantumTjenesteMock.hentFullUttaksplan(saksnummer)).thenReturn(fullUttaksplan);

        var vilkårene = Vilkårene.builder()
            .leggTilIkkeVurderteVilkår(List.of(DatoIntervallEntitet.fra(fom, tom)), List.of(VilkårType.BEREGNINGSGRUNNLAGVILKÅR))
            .build();

        var perioder = finnPerioderSomSkalFjernesIBeregning.finnPerioderSomSkalFjernes(vilkårene, behandlingReferanseMock);

        assertThat(perioder).isEmpty();
    }

    private Uttaksperiode avslått(LocalDate fom, LocalDate tom) {
        return new Uttaksperiode(
            new LukketPeriode(fom, tom),
            Duration.ofHours(1),
            Utfall.AVSLÅTT,
            new VurderteVilkår(Map.of(Vilkår.NOK_DAGER, Utfall.AVSLÅTT)),
            List.of(),
            BigDecimal.ZERO,
            Periodetype.NY,
            tom.atStartOfDay(),
            null,
            Bekreftet.SYSTEMBEKREFTET,
            FraværÅrsak.ORDINÆRT_FRAVÆR,
            SøknadÅrsak.UDEFINERT,
            null,
            true,
            AvvikImSøknad.UDEFINERT,
            null
        );
    }
}
