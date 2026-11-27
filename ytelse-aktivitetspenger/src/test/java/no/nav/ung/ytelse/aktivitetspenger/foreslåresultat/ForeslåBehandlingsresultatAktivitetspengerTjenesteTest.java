package no.nav.ung.ytelse.aktivitetspenger.foreslåresultat;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.kodeverk.vilkår.VilkårsavklaringÅrsaker;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.inngangsvilkår.avklaring.Vilkårsavklaring;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringTjeneste;
import no.nav.ung.ytelse.aktivitetspenger.navkontor.AktivitetspengerVilkårsPerioderTilVurderingTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForeslåBehandlingsresultatAktivitetspengerTjenesteTest {

    private static final long BEHANDLING_ID = 1L;
    private static final DatoIntervallEntitet HELE_PERIODEN = periode(1, 31);

    private final BehandlingRepositoryProvider repositoryProvider = mock(BehandlingRepositoryProvider.class);
    private final BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
    private final VilkårResultatRepository vilkårResultatRepository = mock(VilkårResultatRepository.class);
    private final AktivitetspengerVilkårsPerioderTilVurderingTjeneste perioderTilVurderingTjeneste = mock(AktivitetspengerVilkårsPerioderTilVurderingTjeneste.class);
    private final VilkårsavklaringTjeneste avklaringTjeneste = mock(VilkårsavklaringTjeneste.class);
    private final Behandling behandling = mock(Behandling.class);
    private final BehandlingReferanse referanse = mock(BehandlingReferanse.class);
    private final BehandlingskontrollKontekst kontekst = mock(BehandlingskontrollKontekst.class);

    private ForeslåBehandlingsresultatAktivitetspengerTjeneste tjeneste;

    @BeforeEach
    void setUp() {
        when(repositoryProvider.getBehandlingRepository()).thenReturn(behandlingRepository);
        when(repositoryProvider.getVilkårResultatRepository()).thenReturn(vilkårResultatRepository);
        when(behandlingRepository.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
        when(referanse.getBehandlingId()).thenReturn(BEHANDLING_ID);
        when(perioderTilVurderingTjeneste.definerendeVilkår()).thenReturn(Set.of(VilkårType.ALDERSVILKÅR));
        vurderPerioder(HELE_PERIODEN);
        tjeneste = new ForeslåBehandlingsresultatAktivitetspengerTjeneste(
            repositoryProvider, perioderTilVurderingTjeneste, avklaringTjeneste);
    }

    @Test
    void lagrer_innvilget_når_alle_vilkår_er_oppfylt() {
        foreslåOgVerifiser(Vilkårene.builder()
            .leggTil(vilkår(VilkårType.ALDERSVILKÅR, vurdering(HELE_PERIODEN, Utfall.OPPFYLT)))
            .leggTil(vilkår(VilkårType.BOSTEDSVILKÅR, vurdering(HELE_PERIODEN, Utfall.OPPFYLT)))
            .build(), BehandlingResultatType.INNVILGET);
    }

    @Test
    void lagrer_avslått_når_ett_vilkår_er_avslått_i_hele_vurderingsperioden() {
        foreslåOgVerifiser(Vilkårene.builder()
            .leggTil(vilkår(VilkårType.ALDERSVILKÅR, vurdering(HELE_PERIODEN, Utfall.OPPFYLT)))
            .leggTil(vilkår(VilkårType.BOSTEDSVILKÅR, vurdering(HELE_PERIODEN, Utfall.IKKE_OPPFYLT)))
            .build(), BehandlingResultatType.AVSLÅTT);
    }

    @Test
    void lagrer_avslått_når_ulike_vilkår_gir_avslag_i_hver_sin_del_av_perioden() {
        foreslåOgVerifiser(Vilkårene.builder()
            .leggTil(vilkår(VilkårType.ALDERSVILKÅR,
                vurdering(periode(1, 15), Utfall.IKKE_OPPFYLT),
                vurdering(periode(16, 31), Utfall.OPPFYLT)))
            .leggTil(vilkår(VilkårType.BOSTEDSVILKÅR,
                vurdering(periode(1, 15), Utfall.OPPFYLT),
                vurdering(periode(16, 31), Utfall.IKKE_OPPFYLT)))
            .build(), BehandlingResultatType.AVSLÅTT);
    }

    @Test
    void lagrer_innvilget_når_kun_en_del_av_perioden_er_avslått_uten_opphørsavklaring() {
        foreslåOgVerifiser(Vilkårene.builder()
            .leggTil(vilkår(VilkårType.ALDERSVILKÅR,
                vurdering(periode(1, 15), Utfall.OPPFYLT),
                vurdering(periode(16, 31), Utfall.IKKE_OPPFYLT)))
            .build(), BehandlingResultatType.INNVILGET);
    }

    @Test
    void avslag_utenfor_vurderingsperioden_påvirker_ikke_resultatet() {
        vurderPerioder(periode(16, 31));

        foreslåOgVerifiser(Vilkårene.builder()
            .leggTil(vilkår(VilkårType.ALDERSVILKÅR,
                vurdering(periode(1, 15), Utfall.IKKE_OPPFYLT),
                vurdering(periode(16, 31), Utfall.OPPFYLT)))
            .build(), BehandlingResultatType.INNVILGET);
    }

    @Test
    void innvilgelse_utenfor_vurderingsperioden_hindrer_ikke_avslag() {
        vurderPerioder(periode(16, 31));

        foreslåOgVerifiser(Vilkårene.builder()
            .leggTil(vilkår(VilkårType.ALDERSVILKÅR,
                vurdering(periode(1, 15), Utfall.OPPFYLT),
                vurdering(periode(16, 31), Utfall.IKKE_OPPFYLT)))
            .build(), BehandlingResultatType.AVSLÅTT);
    }

    @Test
    void avslag_i_hull_mellom_vurderingsperiodene_påvirker_ikke_resultatet() {
        vurderPerioder(periode(1, 10), periode(21, 31));

        foreslåOgVerifiser(Vilkårene.builder()
            .leggTil(vilkår(VilkårType.ALDERSVILKÅR,
                vurdering(periode(1, 10), Utfall.OPPFYLT),
                vurdering(periode(11, 20), Utfall.IKKE_OPPFYLT),
                vurdering(periode(21, 31), Utfall.OPPFYLT)))
            .build(), BehandlingResultatType.INNVILGET);
    }

    @ParameterizedTest
    @EnumSource(value = Utfall.class, names = {"OPPFYLT", "IKKE_VURDERT"})
    void utfall_som_ikke_er_avslag_gir_ikke_fullt_avslag(Utfall utfall) {
        var vilkårene = Vilkårene.builder()
            .leggTil(vilkår(VilkårType.ALDERSVILKÅR, vurdering(HELE_PERIODEN, utfall)))
            .build();

        assertThat(tjeneste.skalBehandlingResultatSettesTilAvslått(referanse, vilkårene)).isFalse();
    }

    @Test
    void ingen_perioder_til_vurdering_gir_ikke_avslag() {
        vurderPerioder();
        var fagsak = mock(Fagsak.class);
        when(behandling.getFagsak()).thenReturn(fagsak);
        when(fagsak.getPeriode()).thenReturn(HELE_PERIODEN);
        var vilkårene = Vilkårene.builder()
            .leggTil(vilkår(VilkårType.ALDERSVILKÅR, vurdering(HELE_PERIODEN, Utfall.IKKE_OPPFYLT)))
            .build();

        assertThat(tjeneste.skalBehandlingResultatSettesTilAvslått(referanse, vilkårene)).isFalse();
        assertThat(tjeneste.getMaksPeriode(BEHANDLING_ID)).isEqualTo(HELE_PERIODEN);
    }

    @Test
    void vilkårsutfall_uten_overlapp_med_vurderingsperioden_gir_ikke_avslag() {
        vurderPerioder(periode(16, 31));
        var vilkårene = Vilkårene.builder()
            .leggTil(vilkår(VilkårType.ALDERSVILKÅR, vurdering(periode(1, 15), Utfall.IKKE_OPPFYLT)))
            .build();

        assertThat(tjeneste.skalBehandlingResultatSettesTilAvslått(referanse, vilkårene)).isFalse();
    }

    @Test
    void fullt_avslag_prioriteres_foran_opphør() {
        opphørsavklaring(VilkårType.BOSTEDSVILKÅR, HELE_PERIODEN, Avklaringtype.OPPHØR);

        foreslåOgVerifiser(Vilkårene.builder()
            .leggTil(vilkår(VilkårType.ALDERSVILKÅR, vurdering(HELE_PERIODEN, Utfall.OPPFYLT)))
            .leggTil(vilkår(VilkårType.BOSTEDSVILKÅR, vurdering(HELE_PERIODEN, Utfall.IKKE_OPPFYLT)))
            .build(), BehandlingResultatType.AVSLÅTT);
        verify(avklaringTjeneste, never()).hentSenesteForeslåtteAvklaringForBehandling(eq(BEHANDLING_ID), any());
    }

    @Test
    void maksperiode_omfatter_alle_definerende_vilkår_og_hull_mellom_periodene() {
        when(perioderTilVurderingTjeneste.definerendeVilkår()).thenReturn(Set.of(VilkårType.ALDERSVILKÅR, VilkårType.BOSTEDSVILKÅR));
        vurderPerioder(periode(1, 10));
        when(perioderTilVurderingTjeneste.utled(BEHANDLING_ID, VilkårType.BOSTEDSVILKÅR))
            .thenReturn(new TreeSet<>(List.of(periode(21, 31))));

        assertThat(tjeneste.getMaksPeriode(BEHANDLING_ID)).isEqualTo(HELE_PERIODEN);
    }

    @Test
    void lagrer_opphør_når_seneste_avklaring_er_opphør_med_avslag() {
        opphørsavklaring(VilkårType.BOSTEDSVILKÅR, periode(16, 31), Avklaringtype.OPPHØR);

        foreslåOgVerifiser(Vilkårene.builder()
            .leggTil(vilkår(VilkårType.ALDERSVILKÅR, vurdering(HELE_PERIODEN, Utfall.OPPFYLT)))
            .leggTil(vilkår(VilkårType.BOSTEDSVILKÅR,
                vurdering(periode(1, 15), Utfall.OPPFYLT),
                vurdering(periode(16, 31), Utfall.IKKE_OPPFYLT)))
            .build(), BehandlingResultatType.OPPHØR);
    }

    @ParameterizedTest
    @MethodSource("vilkårMedAvklaringsårsak")
    void opphør_støttes_for_alle_vilkår_med_avklaringsårsak(VilkårType vilkårType) {
        opphørsavklaring(vilkårType, HELE_PERIODEN, Avklaringtype.OPPHØR);
        var vilkårene = Vilkårene.builder()
            .leggTil(vilkår(vilkårType, vurdering(HELE_PERIODEN, Utfall.IKKE_OPPFYLT)))
            .build();

        assertThat(tjeneste.skalBehandlingResultatSettesTilOpphør(referanse, vilkårene)).isTrue();
    }

    @Test
    void avkortet_periode_gir_ikke_opphør() {
        opphørsavklaring(VilkårType.BOSTEDSVILKÅR, HELE_PERIODEN, Avklaringtype.OPPHØR);
        var vilkårene = Vilkårene.builder()
            .leggTil(vilkår(VilkårType.BOSTEDSVILKÅR,
                vurdering(HELE_PERIODEN, Utfall.IKKE_OPPFYLT).medAvslagsårsak(Avslagsårsak.AVKORTET)))
            .build();

        assertThat(tjeneste.skalBehandlingResultatSettesTilOpphør(referanse, vilkårene)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Utfall.class, names = {"OPPFYLT", "IKKE_VURDERT"})
    void vilkår_uten_avslag_gir_ikke_opphør(Utfall utfall) {
        opphørsavklaring(VilkårType.BOSTEDSVILKÅR, HELE_PERIODEN, Avklaringtype.OPPHØR);
        var vilkårene = Vilkårene.builder()
            .leggTil(vilkår(VilkårType.BOSTEDSVILKÅR, vurdering(HELE_PERIODEN, utfall)))
            .build();

        assertThat(tjeneste.skalBehandlingResultatSettesTilOpphør(referanse, vilkårene)).isFalse();
    }

    @Test
    void avslagsavklaring_gir_ikke_opphør() {
        opphørsavklaring(VilkårType.BOSTEDSVILKÅR, HELE_PERIODEN, Avklaringtype.AVSLAG);
        var vilkårene = Vilkårene.builder()
            .leggTil(vilkår(VilkårType.BOSTEDSVILKÅR, vurdering(HELE_PERIODEN, Utfall.IKKE_OPPFYLT)))
            .build();

        assertThat(tjeneste.skalBehandlingResultatSettesTilOpphør(referanse, vilkårene)).isFalse();
    }

    @Test
    void avslag_utenfor_avklaringsperioden_gir_ikke_opphør() {
        opphørsavklaring(VilkårType.BOSTEDSVILKÅR, periode(16, 31), Avklaringtype.OPPHØR);
        var vilkårene = Vilkårene.builder()
            .leggTil(vilkår(VilkårType.BOSTEDSVILKÅR,
                vurdering(periode(1, 15), Utfall.IKKE_OPPFYLT),
                vurdering(periode(16, 31), Utfall.OPPFYLT)))
            .build();

        assertThat(tjeneste.skalBehandlingResultatSettesTilOpphør(referanse, vilkårene)).isFalse();
    }

    @Test
    void avslag_på_et_annet_vilkår_gir_ikke_opphør() {
        opphørsavklaring(VilkårType.BOSTEDSVILKÅR, HELE_PERIODEN, Avklaringtype.OPPHØR);
        var vilkårene = Vilkårene.builder()
            .leggTil(vilkår(VilkårType.BOSTEDSVILKÅR, vurdering(HELE_PERIODEN, Utfall.OPPFYLT)))
            .leggTil(vilkår(VilkårType.BISTANDSVILKÅR, vurdering(HELE_PERIODEN, Utfall.IKKE_OPPFYLT)))
            .build();

        assertThat(tjeneste.skalBehandlingResultatSettesTilOpphør(referanse, vilkårene)).isFalse();
    }

    @Test
    void manglende_avklaring_gir_ikke_opphør() {
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.ENDRET_BOSTED));
        var vilkårene = Vilkårene.builder()
            .leggTil(vilkår(VilkårType.BOSTEDSVILKÅR, vurdering(HELE_PERIODEN, Utfall.IKKE_OPPFYLT)))
            .build();

        assertThat(tjeneste.skalBehandlingResultatSettesTilOpphør(referanse, vilkårene)).isFalse();
    }

    @Test
    void avklaring_uten_tilhørende_behandlingsårsak_gir_ikke_opphør() {
        opphørsavklaring(VilkårType.BOSTEDSVILKÅR, HELE_PERIODEN, Avklaringtype.OPPHØR);
        when(behandling.getBehandlingÅrsakerTyper()).thenReturn(List.of(BehandlingÅrsakType.NY_SØKT_PERIODE));
        var vilkårene = Vilkårene.builder()
            .leggTil(vilkår(VilkårType.BOSTEDSVILKÅR, vurdering(HELE_PERIODEN, Utfall.IKKE_OPPFYLT)))
            .build();

        assertThat(tjeneste.skalBehandlingResultatSettesTilOpphør(referanse, vilkårene)).isFalse();
        verify(avklaringTjeneste, never()).hentSenesteForeslåtteAvklaringForBehandling(eq(BEHANDLING_ID), any());
    }

    private void foreslåOgVerifiser(Vilkårene vilkårene, BehandlingResultatType forventetResultat) {
        when(vilkårResultatRepository.hent(BEHANDLING_ID)).thenReturn(vilkårene);

        tjeneste.foreslåBehandlingsresultatType(referanse, kontekst);

        verify(behandling).setBehandlingResultatType(forventetResultat);
        verify(behandlingRepository).lagre(behandling, kontekst.getSkriveLås());
    }

    private void vurderPerioder(DatoIntervallEntitet... perioder) {
        when(perioderTilVurderingTjeneste.utled(BEHANDLING_ID, VilkårType.ALDERSVILKÅR))
            .thenReturn(new TreeSet<>(Arrays.asList(perioder)));
    }

    private void opphørsavklaring(VilkårType vilkårType, DatoIntervallEntitet periode, Avklaringtype avklaringtype) {
        when(behandling.getBehandlingÅrsakerTyper())
            .thenReturn(List.of(VilkårsavklaringÅrsaker.avklaringsårsakFor(vilkårType).orElseThrow()));
        when(avklaringTjeneste.hentSenesteForeslåtteAvklaringForBehandling(BEHANDLING_ID, vilkårType))
            .thenReturn(Optional.of(new Vilkårsavklaring(avklaringtype, periode, null, null)));
    }

    private static Stream<VilkårType> vilkårMedAvklaringsårsak() {
        return VilkårsavklaringÅrsaker.alle().keySet().stream();
    }

    private static VilkårBuilder vilkår(VilkårType type, VilkårPeriodeBuilder... perioder) {
        var builder = new VilkårBuilder(type);
        Arrays.stream(perioder).forEach(builder::leggTil);
        return builder;
    }

    private static VilkårPeriodeBuilder vurdering(DatoIntervallEntitet periode, Utfall utfall) {
        return new VilkårPeriodeBuilder().medPeriode(periode.getFomDato(), periode.getTomDato())
            .medUtfall(utfall)
            .medAvslagsårsak(utfall == Utfall.IKKE_OPPFYLT ? Avslagsårsak.MANGLENDE_DOKUMENTASJON : null);
    }

    private static DatoIntervallEntitet periode(int fomDag, int tomDag) {
        return DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2026, 1, fomDag), LocalDate.of(2026, 1, tomDag));
    }
}
