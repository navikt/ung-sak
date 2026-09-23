package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserAvklaringKildeType;
import no.nav.ung.kodeverk.vilkår.AndreLivsoppholdsytelserIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.AvklaringKilde;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.BostedsavklaringKildeType;
import no.nav.ung.kodeverk.vilkår.BostedsvilkårIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.IkkeOppfyltDetaljertÅrsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.UtbetalingsgradType;
import no.nav.ung.sak.inngangsvilkår.avklaring.Vilkårsavklaring;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringMedVurdering;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringOgVurderingTidslinjeUtleder;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.EndringAvslagDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.KildeTilOpplysninger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndringAvslagInnholdByggerTest {

    private static final long BEHANDLING_ID = 42L;
    private static final LocalDate FOM = LocalDate.of(2025, 8, 1);
    private static final LocalDate TOM = LocalDate.of(2025, 8, 31);

    @Mock
    private VilkårsavklaringOgVurderingTidslinjeUtleder tidslinjeUtleder;

    @Mock
    private Behandling behandling;

    private EndringAvslagInnholdBygger bygger;

    @BeforeEach
    void setUp() {
        bygger = new EndringAvslagInnholdBygger(tidslinjeUtleder);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
    }

    @DisplayName("Vilkår avklart likt gir ett brev med felles periode og kilde, og en blokk per vilkår")
    @Test
    void likeAvklaringerGirBrev() {
        avklaringerErUtledet(bosted(FOM, TOM, Avklaringtype.OPPHØR, BostedsavklaringKildeType.BRUKER),
            livsopphold(FOM, TOM, Avklaringtype.OPPHØR, AndreLivsoppholdsytelserAvklaringKildeType.BRUKER));

        var resultat = bygger.bygg(behandling, avslåtteVilkårTidslinje(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, VilkårType.BOSTEDSVILKÅR));

        assertThat(resultat.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_OPPHØR);
        var dto = (EndringAvslagDto) resultat.templateInnholdDto();
        assertThat(dto.periode()).isEqualTo(new Periode(FOM, TOM));
        assertThat(dto.kilde()).isEqualTo(KildeTilOpplysninger.nyFraBruker());
        assertThat(dto.bosted()).isNotNull();
        assertThat(dto.andreLivsoppholdsytelser()).isNotNull();
    }

    @DisplayName("Vilkår som ikke er avslått får null, og malen skriver da ingenting om det")
    @Test
    void kunEttAvslåttVilkår() {
        avklaringerErUtledet(bosted(FOM, TOM, Avklaringtype.AVSLAG, BostedsavklaringKildeType.FOLKEREGISTER));

        var resultat = bygger.bygg(behandling, avslåtteVilkårTidslinje(VilkårType.BOSTEDSVILKÅR));

        assertThat(resultat.templateType()).isEqualTo(TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG);
        var dto = (EndringAvslagDto) resultat.templateInnholdDto();
        assertThat(dto.kilde()).isEqualTo(KildeTilOpplysninger.nyFraFolkeregisteret());
        assertThat(dto.bosted()).isNotNull();
        assertThat(dto.andreLivsoppholdsytelser()).isNull();
    }

    @DisplayName("Avklaringtypen velger mal, og kan derfor ikke være ulik")
    @Test
    void ulikAvklaringtype() {
        avklaringerErUtledet(bosted(FOM, TOM, Avklaringtype.OPPHØR, BostedsavklaringKildeType.BRUKER),
            livsopphold(FOM, TOM, Avklaringtype.AVSLAG, AndreLivsoppholdsytelserAvklaringKildeType.BRUKER));

        assertThatThrownBy(() -> bygger.bygg(behandling, avslåtteVilkårTidslinje(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, VilkårType.BOSTEDSVILKÅR)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("avklart ulikt");
    }

    @DisplayName("Innledningssetningen skrives én gang, så perioden kan ikke være ulik")
    @Test
    void ulikPeriode() {
        avklaringerErUtledet(bosted(FOM, TOM, Avklaringtype.OPPHØR, BostedsavklaringKildeType.BRUKER),
            livsopphold(FOM.plusDays(1), TOM, Avklaringtype.OPPHØR, AndreLivsoppholdsytelserAvklaringKildeType.BRUKER));

        assertThatThrownBy(() -> bygger.bygg(behandling, avslåtteVilkårTidslinje(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, VilkårType.BOSTEDSVILKÅR)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("avklart ulikt");
    }

    @DisplayName("Kildeavsnittet skrives én gang, så kilden kan ikke være ulik")
    @Test
    void ulikKilde() {
        avklaringerErUtledet(bosted(FOM, TOM, Avklaringtype.OPPHØR, BostedsavklaringKildeType.BRUKER),
            livsopphold(FOM, TOM, Avklaringtype.OPPHØR, AndreLivsoppholdsytelserAvklaringKildeType.NAV));

        assertThatThrownBy(() -> bygger.bygg(behandling, avslåtteVilkårTidslinje(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, VilkårType.BOSTEDSVILKÅR)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("avklart ulikt");
    }

    @SafeVarargs
    private void avklaringerErUtledet(Map.Entry<VilkårType, LocalDateSegment<VilkårsavklaringMedVurdering>>... avklaringer) {
        var tidslinje = Arrays.stream(avklaringer)
            .map(avklaring -> new LocalDateTimeline<>(avklaring.getValue().getLocalDateInterval(),
                Map.of(avklaring.getKey(), avklaring.getValue().getValue())))
            .reduce(LocalDateTimeline.<Map<VilkårType, VilkårsavklaringMedVurdering>>empty(),
                (akkumulert, neste) -> akkumulert.combine(neste, (interval, venstre, høyre) -> {
                    Map<VilkårType, VilkårsavklaringMedVurdering> slåttSammen = new LinkedHashMap<>();
                    if (venstre != null) {
                        slåttSammen.putAll(venstre.getValue());
                    }
                    if (høyre != null) {
                        slåttSammen.putAll(høyre.getValue());
                    }
                    return new LocalDateSegment<>(interval, slåttSammen);
                }, LocalDateTimeline.JoinStyle.CROSS_JOIN));

        when(tidslinjeUtleder.utled(BEHANDLING_ID)).thenReturn(tidslinje);
    }

    private static Map.Entry<VilkårType, LocalDateSegment<VilkårsavklaringMedVurdering>> bosted(LocalDate fom, LocalDate tom, Avklaringtype avklaringtype, AvklaringKilde kilde) {
        return avklaring(VilkårType.BOSTEDSVILKÅR, fom, tom, avklaringtype, kilde,
            BostedsvilkårIkkeOppfyltÅrsak.IKKE_BOSATTADRESSE_I_TRONDHEIM);
    }

    private static Map.Entry<VilkårType, LocalDateSegment<VilkårsavklaringMedVurdering>> livsopphold(LocalDate fom, LocalDate tom, Avklaringtype avklaringtype, AvklaringKilde kilde) {
        return avklaring(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, fom, tom, avklaringtype, kilde,
            AndreLivsoppholdsytelserIkkeOppfyltÅrsak.MOTTAR_DAGPENGER);
    }

    private static Map.Entry<VilkårType, LocalDateSegment<VilkårsavklaringMedVurdering>> avklaring(VilkårType vilkårType,
                                                                                                    LocalDate fom,
                                                                                                    LocalDate tom,
                                                                                                    Avklaringtype avklaringtype,
                                                                                                    AvklaringKilde kilde,
                                                                                                    IkkeOppfyltDetaljertÅrsak ikkeOppfyltÅrsak) {
        var vilkårsavklaring = new Vilkårsavklaring(avklaringtype, DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), kilde, null);
        var vurdering = new VilkårsvurderingResultat(vilkårType, false, ikkeOppfyltÅrsak, true,
            "Begrunnelse fra test", null, "A111111", LocalDateTime.now());
        var medVurdering = new VilkårsavklaringMedVurdering(vilkårType, BehandlingÅrsakType.UDEFINERT, vilkårsavklaring, vurdering);
        return Map.entry(vilkårType, new LocalDateSegment<>(fom, tom, medVurdering));
    }

    private static DetaljertResultatTidslinje avslåtteVilkårTidslinje(VilkårType... vilkårTyper) {
        var avslåtteVilkår = Arrays.stream(vilkårTyper)
            .map(vilkårType -> new DetaljertVilkårResultat(Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED, vilkårType, Utfall.IKKE_OPPFYLT))
            .collect(Collectors.toSet());

        var resultat = new DetaljertResultat(Set.of(), avslåtteVilkår, Set.of(), UtbetalingsgradType.INGEN_UTBETALING, true);
        return DetaljertResultatTidslinje.av(new LocalDateTimeline<>(FOM, TOM, resultat));
    }
}
