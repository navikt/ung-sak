package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.medisinsk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.PåTversAvHelgErKantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.medlem.kontrollerfakta.AksjonspunktutlederForMedlemskap;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.perioder.VilkårsPerioderTilVurderingTjeneste;
import no.nav.k9.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.k9.sak.ytelse.beregning.grunnlag.BeregningPerioderGrunnlagRepository;
import no.nav.k9.sak.ytelse.pleiepengerbarn.uttak.OverstyrUttakTjeneste;

class FastsettSkjæringstidspunkterForYtelseStegTest {

    private BehandlingRepositoryProvider mockProvider = mock(BehandlingRepositoryProvider.class); // Brukes ikke, men kan ikke være null
    private BeregningPerioderGrunnlagRepository mockrep = mock(BeregningPerioderGrunnlagRepository.class); // Brukes ikke, men kan ikke være null
    private AksjonspunktutlederForMedlemskap mockUtleder = mock(AksjonspunktutlederForMedlemskap.class); // Brukes ikke, men kan ikke være null

    private OverstyrUttakTjeneste mockOverstyrUttakTjeneste = mock(OverstyrUttakTjeneste.class); // Brukes ikke, men kan ikke være null
    private final VilkårsPerioderTilVurderingTjeneste vilkårsPerioderTilVurderingTjeneste = new VilkårsPerioderTilVurderingTjeneste() {
        @Override
        public NavigableSet<DatoIntervallEntitet> utled(Long behandlingId, VilkårType vilkårType) {
            return null;
        }

        @Override
        public Map<VilkårType, NavigableSet<DatoIntervallEntitet>> utledRådataTilUtledningAvVilkårsperioder(Long behandlingId) {
            return null;
        }

        @Override
        public int maksMellomliggendePeriodeAvstand() {
            return 0;
        }

        @Override
        public Set<VilkårType> definerendeVilkår() {
            return Set.of(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR, VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        }

        @Override
        public KantIKantVurderer getKantIKantVurderer() {
            return new PåTversAvHelgErKantIKantVurderer();
        }
    };
    private UnitTestLookupInstanceImpl<VilkårsPerioderTilVurderingTjeneste> instance = new UnitTestLookupInstanceImpl<>(vilkårsPerioderTilVurderingTjeneste);
    private FastsettSkjæringstidspunkterForYtelseSteg steg = new FastsettSkjæringstidspunkterForYtelseSteg(mockProvider, mockrep, mockOverstyrUttakTjeneste, instance, mockUtleder);

    @Test
    void skal_ikke_justere_utfall_for_andre_vilkår_ved_perioder_med_avslag_på_medisinsk_dersom_kun_helg_og_kopiert_vilkårsresultat() {
        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        var oppfyltPeriode1 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 1));
        var avslåttPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024, 3, 2), LocalDate.of(2024, 3, 3));
        var oppfyltPeriode2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024, 3, 4), LocalDate.of(2024, 3, 4));
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(oppfyltPeriode1.getFomDato(), oppfyltPeriode2.getTomDato());
        var perioderTilVurdering = List.of(periodeTilVurdering);

        // Simulering av kopiert resultat
        settOppfylt(builder, periodeTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        settOppfylt(builder, periodeTilVurdering, VilkårType.MEDLEMSKAPSVILKÅRET);
        settOppfylt(builder, periodeTilVurdering, VilkårType.OPPTJENINGSPERIODEVILKÅR);
        settOppfylt(builder, periodeTilVurdering, VilkårType.OPPTJENINGSVILKÅRET);

        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(oppfyltPeriode1)
                .medUtfall(Utfall.OPPFYLT))
            .leggTil(vilkårBuilder.hentBuilderFor(avslåttPeriode)
                .medUtfall(Utfall.IKKE_OPPFYLT))
            .leggTil(vilkårBuilder.hentBuilderFor(oppfyltPeriode2)
                .medUtfall(Utfall.OPPFYLT));
        builder.leggTil(vilkårBuilder);

        var vilkårBuilder18år = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        builder.leggTil(vilkårBuilder18år);

        var resultatBuilder = steg.justerVilkårsperioderEtterDefinerendeVilkår(builder.build(), new TreeSet<>(perioderTilVurdering), vilkårsPerioderTilVurderingTjeneste);

        var oppdaterteVilkår = resultatBuilder.build();

        assertThat(oppdaterteVilkår).isNotNull();

        for (Vilkår vilkår : oppdaterteVilkår.getVilkårene()) {
            if (VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(3);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(oppfyltPeriode1, avslåttPeriode, oppfyltPeriode2);
            } else if (VilkårType.MEDISINSKEVILKÅR_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).isEmpty();
            } else {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().get(0).getPeriode()).isEqualTo(periodeTilVurdering);
            }
        }
    }

    private static void settOppfylt(VilkårResultatBuilder builder, DatoIntervallEntitet periodeTilVurdering, VilkårType vilkårType) {
        var vilkårBuilder1 = builder.hentBuilderFor(vilkårType);
        vilkårBuilder1.leggTil(vilkårBuilder1.hentBuilderFor(periodeTilVurdering)
            .medUtfall(Utfall.OPPFYLT));
    }


    @Test
    void skal_justere_utfall_ved_perioder_med_avslag_på_medisinsk() {
        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        var oppfyltPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(28), LocalDate.now());
        var avslåttPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(32), LocalDate.now().minusDays(29));
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(avslåttPeriode.getFomDato(), oppfyltPeriode.getTomDato());
        var perioderTilVurdering = List.of(periodeTilVurdering);

        builder.leggTilIkkeVurderteVilkår(perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            VilkårType.OPPTJENINGSPERIODEVILKÅR,
            VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(oppfyltPeriode)
                .medUtfall(Utfall.OPPFYLT))
            .leggTil(vilkårBuilder.hentBuilderFor(avslåttPeriode)
                .medUtfall(Utfall.IKKE_OPPFYLT));
        builder.leggTil(vilkårBuilder);

        var vilkårBuilder18år = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        builder.leggTil(vilkårBuilder18år);

        var resultatBuilder = steg.justerVilkårsperioderEtterDefinerendeVilkår(builder.build(), new TreeSet<>(perioderTilVurdering), vilkårsPerioderTilVurderingTjeneste);

        var oppdaterteVilkår = resultatBuilder.build();

        assertThat(oppdaterteVilkår).isNotNull();

        for (Vilkår vilkår : oppdaterteVilkår.getVilkårene()) {
            if (VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(2);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(oppfyltPeriode, avslåttPeriode);
            } else if (VilkårType.MEDISINSKEVILKÅR_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).isEmpty();
            } else {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().get(0).getPeriode()).isEqualTo(oppfyltPeriode);
            }
        }
    }

    @Test
    void skal_justere_utfall_ved_perioder_med_avslag_på_medisinsk_med_18årsvurdering() {
        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        var oppfyltPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(28), LocalDate.now());
        var avslåttPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(32), LocalDate.now().minusDays(29));
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(avslåttPeriode.getFomDato(), oppfyltPeriode.getTomDato());
        var perioderTilVurdering = List.of(periodeTilVurdering);

        builder.leggTilIkkeVurderteVilkår(perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            VilkårType.OPPTJENINGSPERIODEVILKÅR,
            VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(oppfyltPeriode)
                .medUtfall(Utfall.OPPFYLT))
            .leggTil(vilkårBuilder.hentBuilderFor(avslåttPeriode)
                .medUtfall(Utfall.IKKE_OPPFYLT));
        builder.leggTil(vilkårBuilder);

        var vilkårBuilder18år = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        builder.leggTil(vilkårBuilder18år);

        var resultatBuilder = steg.justerVilkårsperioderEtterDefinerendeVilkår(builder.build(), new TreeSet<>(perioderTilVurdering), vilkårsPerioderTilVurderingTjeneste);

        var oppdaterteVilkår = resultatBuilder.build();

        assertThat(oppdaterteVilkår).isNotNull();

        for (Vilkår vilkår : oppdaterteVilkår.getVilkårene()) {
            if (VilkårType.MEDISINSKEVILKÅR_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(2);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(oppfyltPeriode, avslåttPeriode);
            } else if (VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).isEmpty();
            } else {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().get(0).getPeriode()).isEqualTo(oppfyltPeriode);
            }
        }
    }


    @Test
    void skal_justere_utfall_ved_perioder_med_avslag_på_medisinsk_med_18årsvurdering_2() {
        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        var avslåttPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(28), LocalDate.now().minusDays(14));
        var avslåttPeriodeDel2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(13), LocalDate.now());
        var oppfyltPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(32), LocalDate.now().minusDays(29));
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(oppfyltPeriode.getFomDato(), avslåttPeriodeDel2.getTomDato());
        var perioderTilVurdering = List.of(periodeTilVurdering);

        builder.leggTilIkkeVurderteVilkår(perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            VilkårType.OPPTJENINGSPERIODEVILKÅR,
            VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(oppfyltPeriode)
                .medUtfall(Utfall.OPPFYLT))
            .leggTil(vilkårBuilder.hentBuilderFor(avslåttPeriode)
                .medUtfall(Utfall.IKKE_OPPFYLT));
        builder.leggTil(vilkårBuilder);

        var vilkårBuilder18år = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        vilkårBuilder18år.leggTil(vilkårBuilder18år.hentBuilderFor(avslåttPeriodeDel2)
            .medUtfall(Utfall.IKKE_OPPFYLT));
        builder.leggTil(vilkårBuilder18år);

        var resultatBuilder = steg.justerVilkårsperioderEtterDefinerendeVilkår(builder.build(), new TreeSet<>(perioderTilVurdering), vilkårsPerioderTilVurderingTjeneste);

        var oppdaterteVilkår = resultatBuilder.build();

        assertThat(oppdaterteVilkår).isNotNull();

        for (Vilkår vilkår : oppdaterteVilkår.getVilkårene()) {
            if (VilkårType.MEDISINSKEVILKÅR_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(avslåttPeriodeDel2);
            } else if (VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(2);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(oppfyltPeriode, avslåttPeriode);
            } else {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().get(0).getPeriode()).isEqualTo(oppfyltPeriode);
            }
        }
    }

    @Test
    void skal_legge_tilbake_periode_hvis_sykdom_godkjent_igjen() {
        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        var oppfyltPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(28), LocalDate.now().minusDays(20));
        var avslåttPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(19), LocalDate.now().minusDays(15));
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(oppfyltPeriode.getFomDato(), avslåttPeriode.getTomDato());
        var perioderTilVurdering = List.of(periodeTilVurdering);

        builder.leggTilIkkeVurderteVilkår(List.of(oppfyltPeriode), VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            VilkårType.OPPTJENINGSPERIODEVILKÅR,
            VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder.leggTil(vilkårBuilder.hentBuilderFor(periodeTilVurdering)
            .medUtfall(Utfall.OPPFYLT));
        builder.leggTil(vilkårBuilder);

        var vilkårBuilder18år = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        builder.leggTil(vilkårBuilder18år);

        var resultatBuilder = steg.justerVilkårsperioderEtterDefinerendeVilkår(builder.build(), new TreeSet<>(perioderTilVurdering), vilkårsPerioderTilVurderingTjeneste);

        var oppdaterteVilkår = resultatBuilder.build();

        assertThat(oppdaterteVilkår).isNotNull();

        for (Vilkår vilkår : oppdaterteVilkår.getVilkårene()) {
            if (VilkårType.MEDISINSKEVILKÅR_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(periodeTilVurdering);
            } else if (VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).isEmpty();
            } else {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().get(0).getPeriode()).isEqualTo(periodeTilVurdering);
            }
        }
    }

    @Test
    void skal_justere_utfall_ved_fullstendig_avslag_på_medisinsk() {
        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        var avslåttPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(32), LocalDate.now().minusDays(29));
        var periodeTilVurdering = avslåttPeriode;
        var perioderTilVurdering = List.of(periodeTilVurdering);

        builder.leggTilIkkeVurderteVilkår(perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            VilkårType.OPPTJENINGSPERIODEVILKÅR,
            VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder
            .leggTil(vilkårBuilder.hentBuilderFor(avslåttPeriode)
                .medUtfall(Utfall.IKKE_OPPFYLT));
        builder.leggTil(vilkårBuilder);

        var vilkårBuilder18år = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        builder.leggTil(vilkårBuilder18år);

        var resultatBuilder = steg.justerVilkårsperioderEtterDefinerendeVilkår(builder.build(), new TreeSet<>(perioderTilVurdering), vilkårsPerioderTilVurderingTjeneste);

        var oppdaterteVilkår = resultatBuilder.build();

        assertThat(oppdaterteVilkår).isNotNull();

        for (Vilkår vilkår : oppdaterteVilkår.getVilkårene()) {
            if (VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(avslåttPeriode);
            } else if (VilkårType.MEDISINSKEVILKÅR_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).isEmpty();
            } else {
                assertThat(vilkår.getPerioder()).isEmpty();
            }
        }
    }

    @Test
    void skal_IKKE_justere_utfall_ved_fullstendig_innvilgselse_på_medisinsk() {
        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        var oppfyltPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(28), LocalDate.now());
        var periodeTilVurdering = oppfyltPeriode;
        var perioderTilVurdering = List.of(periodeTilVurdering);

        builder.leggTilIkkeVurderteVilkår(perioderTilVurdering, VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            VilkårType.OPPTJENINGSPERIODEVILKÅR,
            VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder
            .leggTil(vilkårBuilder.hentBuilderFor(oppfyltPeriode)
                .medUtfall(Utfall.OPPFYLT));
        builder.leggTil(vilkårBuilder);

        var vilkårBuilder18år = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        builder.leggTil(vilkårBuilder18år);

        var resultatBuilder = steg.justerVilkårsperioderEtterDefinerendeVilkår(builder.build(), new TreeSet<>(perioderTilVurdering), vilkårsPerioderTilVurderingTjeneste);

        var oppdaterteVilkår = resultatBuilder.build();

        assertThat(oppdaterteVilkår).isNotNull();

        for (Vilkår vilkår : oppdaterteVilkår.getVilkårene()) {
            if (VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(oppfyltPeriode);
            } else if (VilkårType.MEDISINSKEVILKÅR_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).isEmpty();
            } else {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().get(0).getPeriode()).isEqualTo(periodeTilVurdering);
            }
        }
    }

    @Test
    void skal_IKKE_justere_utfall_ved_fullstendig_innvilgselse_på_medisinsk_men_koble_sammen_hvis_to_vurderinger_kant_i_kant() {
        var builder = Vilkårene.builder();
        var vilkårBuilder = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR);
        var oppfyltPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(28), LocalDate.now().minusDays(14));
        var oppfyltPeriode2 = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(13), LocalDate.now());
        var periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(oppfyltPeriode.getFomDato(), oppfyltPeriode2.getTomDato());
        var perioderTilVurdering = List.of(periodeTilVurdering);

        builder.leggTilIkkeVurderteVilkår(List.of(), VilkårType.BEREGNINGSGRUNNLAGVILKÅR,
            VilkårType.MEDLEMSKAPSVILKÅRET,
            VilkårType.OPPTJENINGSPERIODEVILKÅR,
            VilkårType.OPPTJENINGSVILKÅRET);
        vilkårBuilder
            .leggTil(vilkårBuilder.hentBuilderFor(oppfyltPeriode)
                .medUtfall(Utfall.OPPFYLT))
            .leggTil(vilkårBuilder.hentBuilderFor(oppfyltPeriode2)
                .medUtfall(Utfall.OPPFYLT));
        builder.leggTil(vilkårBuilder);

        var vilkårBuilder18år = builder.hentBuilderFor(VilkårType.MEDISINSKEVILKÅR_18_ÅR);
        builder.leggTil(vilkårBuilder18år);

        var resultatBuilder = steg.justerVilkårsperioderEtterDefinerendeVilkår(builder.build(), new TreeSet<>(perioderTilVurdering), vilkårsPerioderTilVurderingTjeneste);

        var oppdaterteVilkår = resultatBuilder.build();

        assertThat(oppdaterteVilkår).isNotNull();

        for (Vilkår vilkår : oppdaterteVilkår.getVilkårene()) {
            if (VilkårType.MEDISINSKEVILKÅR_UNDER_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).hasSize(2);
                assertThat(vilkår.getPerioder().stream().map(VilkårPeriode::getPeriode)).contains(oppfyltPeriode, oppfyltPeriode2);
            } else if (VilkårType.MEDISINSKEVILKÅR_18_ÅR.equals(vilkår.getVilkårType())) {
                assertThat(vilkår.getPerioder()).isEmpty();
            } else {
                assertThat(vilkår.getPerioder()).hasSize(1);
                assertThat(vilkår.getPerioder().get(0).getPeriode()).isEqualTo(periodeTilVurdering);
            }
        }
    }
}
