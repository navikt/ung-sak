package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårPeriodeResultatDto;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VedtakEndringSammenlignerTest {

    private static final long BEHANDLING_ID = 2L;
    private static final long ORIGINAL_BEHANDLING_ID = 1L;
    private static final LocalDate FOM = LocalDate.of(2025, 8, 1);
    private static final LocalDate TOM = LocalDate.of(2025, 8, 31);

    private static final LocalDateTimeline<Boolean> AVGRENSNING = new LocalDateTimeline<>(FOM, TOM, Boolean.TRUE);

    @Mock
    private VilkårResultatRepository vilkårResultatRepository;

    @Mock
    private TilkjentYtelseRepository tilkjentYtelseRepository;

    @Mock
    private Behandling behandling;

    private VedtakEndringSammenligner sammenligner;

    @BeforeEach
    void setUp() {
        sammenligner = new VedtakEndringSammenligner(vilkårResultatRepository, tilkjentYtelseRepository);
        lenient().when(behandling.getId()).thenReturn(BEHANDLING_ID);
        lenient().when(behandling.getOriginalBehandlingId()).thenReturn(Optional.of(ORIGINAL_BEHANDLING_ID));
    }

    @DisplayName("Uten originalbehandling finnes det ikke noe forrige vedtak å sammenligne med")
    @Test
    void utenOriginalbehandling() {
        when(behandling.getOriginalBehandlingId()).thenReturn(Optional.empty());

        assertThat(sammenligner.sammenlignMedOriginal(behandling, AVGRENSNING)).isEmpty();
    }

    @DisplayName("Like vilkår og lik tilkjent ytelse er uendret")
    @Test
    void identiskeVedtak() {
        vilkårErLagret(ORIGINAL_BEHANDLING_ID, oppfylt(FOM, TOM));
        vilkårErLagret(BEHANDLING_ID, oppfylt(FOM, TOM));
        tilkjentYtelseErLagret(ORIGINAL_BEHANDLING_ID, tilkjentYtelse("500", "10000"));
        tilkjentYtelseErLagret(BEHANDLING_ID, tilkjentYtelse("500", "10000"));

        assertThat(endring().erUendret()).isTrue();
    }

    @DisplayName("Endret utfall på et vilkår er en endring")
    @Test
    void endretUtfall() {
        vilkårErLagret(ORIGINAL_BEHANDLING_ID, oppfylt(FOM, TOM));
        vilkårErLagret(BEHANDLING_ID, avslått(FOM, TOM, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED));
        likTilkjentYtelse();

        var endring = endring();
        assertThat(endring.erUendret()).isFalse();
        assertThat(endring.endredeVilkår().getLocalDateIntervals()).hasSize(1);
    }

    @DisplayName("Samme utfall med ny avslagsårsak er en endring - deltakeren får avslag av en annen grunn")
    @Test
    void endretAvslagsårsak() {
        vilkårErLagret(ORIGINAL_BEHANDLING_ID, avslått(FOM, TOM, Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED));
        vilkårErLagret(BEHANDLING_ID, avslått(FOM, TOM, Avslagsårsak.IKKE_14A_VEDTAK));
        likTilkjentYtelse();

        assertThat(endring().erUendret()).isFalse();
    }

    @DisplayName("Samme beløp med ulik skala er samme beløp")
    @Test
    void ulikSkalaPåSammeBeløp() {
        likeVilkår();
        tilkjentYtelseErLagret(ORIGINAL_BEHANDLING_ID, tilkjentYtelse("500.00", "10000.00"));
        tilkjentYtelseErLagret(BEHANDLING_ID, tilkjentYtelse("500.0", "10000.0"));

        assertThat(endring().erUendret()).isTrue();
    }

    @DisplayName("Endret dagsats er en endring selv om vilkårene er like")
    @Test
    void endretDagsats() {
        likeVilkår();
        tilkjentYtelseErLagret(ORIGINAL_BEHANDLING_ID, tilkjentYtelse("500", "10000"));
        tilkjentYtelseErLagret(BEHANDLING_ID, tilkjentYtelse("600", "10000"));

        var endring = endring();
        assertThat(endring.erUendret()).isFalse();
        assertThat(endring.endredeVilkår()).isEmpty();
        assertThat(endring.endretTilkjentYtelse().getLocalDateIntervals()).hasSize(1);
    }

    @DisplayName("Et vilkår som kun finnes i én av behandlingene er en endring")
    @Test
    void vilkårKunIÉnBehandling() {
        vilkårErLagret(ORIGINAL_BEHANDLING_ID, oppfylt(FOM, TOM));
        vilkårErLagret(BEHANDLING_ID, oppfylt(FOM, TOM),
            new VilkårPeriodeResultatDto(VilkårType.BISTANDSVILKÅR, new Periode(FOM, TOM), null, Utfall.OPPFYLT));
        likTilkjentYtelse();

        assertThat(endring().erUendret()).isFalse();
    }

    @DisplayName("Endringer utenfor avgrensningen teller ikke")
    @Test
    void endringUtenforAvgrensningen() {
        vilkårErLagret(ORIGINAL_BEHANDLING_ID, oppfylt(FOM, TOM), oppfylt(TOM.plusDays(1), TOM.plusMonths(1)));
        vilkårErLagret(BEHANDLING_ID, oppfylt(FOM, TOM),
            avslått(TOM.plusDays(1), TOM.plusMonths(1), Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED));
        likTilkjentYtelse();

        assertThat(endring().erUendret()).isTrue();
    }

    private VedtakEndring endring() {
        return sammenligner.sammenlignMedOriginal(behandling, AVGRENSNING).orElseThrow();
    }

    private void likeVilkår() {
        vilkårErLagret(ORIGINAL_BEHANDLING_ID, oppfylt(FOM, TOM));
        vilkårErLagret(BEHANDLING_ID, oppfylt(FOM, TOM));
    }

    private void likTilkjentYtelse() {
        tilkjentYtelseErLagret(ORIGINAL_BEHANDLING_ID, tilkjentYtelse("500", "10000"));
        tilkjentYtelseErLagret(BEHANDLING_ID, tilkjentYtelse("500", "10000"));
    }

    private void vilkårErLagret(long behandlingId, VilkårPeriodeResultatDto... resultater) {
        when(vilkårResultatRepository.hentVilkårResultater(behandlingId)).thenReturn(List.of(resultater));
    }

    private void tilkjentYtelseErLagret(long behandlingId, TilkjentYtelseVerdi verdi) {
        when(tilkjentYtelseRepository.hentTidslinje(behandlingId)).thenReturn(new LocalDateTimeline<>(FOM, TOM, verdi));
    }

    private static VilkårPeriodeResultatDto oppfylt(LocalDate fom, LocalDate tom) {
        return new VilkårPeriodeResultatDto(VilkårType.BOSTEDSVILKÅR, new Periode(fom, tom), null, Utfall.OPPFYLT);
    }

    private static VilkårPeriodeResultatDto avslått(LocalDate fom, LocalDate tom, Avslagsårsak avslagsårsak) {
        return new VilkårPeriodeResultatDto(VilkårType.BOSTEDSVILKÅR, new Periode(fom, tom), avslagsårsak, Utfall.IKKE_OPPFYLT);
    }

    private static TilkjentYtelseVerdi tilkjentYtelse(String dagsats, String tilkjentBeløp) {
        return new TilkjentYtelseVerdi(new BigDecimal(tilkjentBeløp), BigDecimal.ZERO, new BigDecimal(tilkjentBeløp),
            new BigDecimal(dagsats), new BigDecimal("100"), new BigDecimal(tilkjentBeløp));
    }
}
