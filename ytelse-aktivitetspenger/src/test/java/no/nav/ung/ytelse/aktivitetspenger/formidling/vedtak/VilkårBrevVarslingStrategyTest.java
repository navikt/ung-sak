package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.vilkår.AktivitetsvilkåretIkkeOppfyltÅrsak;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.GenereltVilkårsvurderingResultat;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.resultat.DagsatsEndringType;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.UtbetalingsgradType;
import no.nav.ung.sak.formidling.vedtak.resultat.VedtakEndringSammenligner;
import no.nav.ung.sak.formidling.vedtak.resultat.VedtakSammenligningResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.VilkårEndringType;
import no.nav.ung.sak.inngangsvilkår.avklaring.Vilkårsavklaring;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringMedVurdering;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringOgVurderingTidslinjeUtleder;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.EndringAvslagInnholdBygger;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.UendretInnholdBygger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VilkårBrevVarslingStrategyTest {

    private static final long BEHANDLING_ID = 42L;
    private static final LocalDate FOM = LocalDate.of(2025, 8, 1);
    private static final LocalDate TOM = LocalDate.of(2025, 8, 31);

    @Mock
    private Behandling behandling;
    @Mock
    private VilkårsavklaringOgVurderingTidslinjeUtleder tidslinjeUtleder;
    @Mock
    private VedtakEndringSammenligner vedtakEndringSammenligner;
    @Mock
    private EndringAvslagInnholdBygger endringAvslagInnholdBygger;
    @Mock
    private UendretInnholdBygger uendretInnholdBygger;

    @Test
    void faktiskAvslagGirBrevSelvOmVarslingErFravalgt() {
        var strategi = new EndringAvslagStrategy(endringAvslagInnholdBygger, tidslinjeUtleder);
        var avklaring = avklaring(false, false);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
        when(tidslinjeUtleder.utled(BEHANDLING_ID)).thenReturn(tidslinje(avklaring));

        var resultat = strategi.evaluer(behandling, resultatTidslinje(true));

        assertThat(resultat).singleElement().satisfies(it -> {
            assertThat(it.dokumentMalType()).isEqualTo(DokumentMalType.OPPHØR_DOK);
            assertThat(it.bygger()).isSameAs(endringAvslagInnholdBygger);
        });
    }

    @Test
    void uendretVedtakSomIkkeSkalHaBrevGirIngenBrevResultat() {
        var strategi = new UendretStrategy(uendretInnholdBygger, tidslinjeUtleder, vedtakEndringSammenligner);
        var avklaring = avklaring(true, false);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
        when(tidslinjeUtleder.utled(BEHANDLING_ID)).thenReturn(tidslinje(avklaring));
        when(vedtakEndringSammenligner.sammenlignMedOriginal(any(), any()))
            .thenReturn(Optional.of(vedtakSammenligningResultat(true)));

        var resultat = strategi.evaluer(behandling, resultatTidslinje(false));

        assertThat(resultat).singleElement().satisfies(it -> {
            assertThat(it.bygger()).isNull();
            assertThat(it.ingenBrevÅrsakType()).isEqualTo(IngenBrevÅrsakType.IKKE_RELEVANT);
        });
    }

    @Test
    void fravalgtVarselUndertrykkerIkkeEndretVedtak() {
        var strategi = new UendretStrategy(uendretInnholdBygger, tidslinjeUtleder, vedtakEndringSammenligner);
        var avklaring = avklaring(true, false);
        when(behandling.getId()).thenReturn(BEHANDLING_ID);
        when(tidslinjeUtleder.utled(BEHANDLING_ID)).thenReturn(tidslinje(avklaring));
        when(vedtakEndringSammenligner.sammenlignMedOriginal(any(), any()))
            .thenReturn(Optional.of(vedtakSammenligningResultat(false)));

        var resultat = strategi.evaluer(behandling, resultatTidslinje(false));

        assertThat(resultat).isEmpty();
    }

    private static LocalDateTimeline<Map<VilkårType, VilkårsavklaringMedVurdering>> tidslinje(VilkårsavklaringMedVurdering avklaring) {
        return new LocalDateTimeline<>(FOM, TOM, Map.of(VilkårType.AKTIVITETSVILKÅR, avklaring));
    }

    private static VilkårsavklaringMedVurdering avklaring(boolean godkjent, boolean skalSendeVarsel) {
        var årsak = godkjent ? null : AktivitetsvilkåretIkkeOppfyltÅrsak.ANNET;
        var vurdering = new GenereltVilkårsvurderingResultat(
            VilkårType.AKTIVITETSVILKÅR,
            godkjent,
            årsak,
            true,
            "begrunnelse",
            godkjent ? null : "fritekst",
            "A111111",
            LocalDateTime.of(2025, 8, 1, 12, 0),
            skalSendeVarsel
        );
        var avklaring = new Vilkårsavklaring(
            Avklaringtype.OPPHØR,
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TOM),
            null,
            null
        );
        return new VilkårsavklaringMedVurdering(
            VilkårType.AKTIVITETSVILKÅR,
            BehandlingÅrsakType.ENDRET_AKTIVITET,
            avklaring,
            vurdering
        );
    }

    private static DetaljertResultatTidslinje resultatTidslinje(boolean avslått) {
        var avslåtteVilkår = avslått
            ? Set.of(new DetaljertVilkårResultat(Avslagsårsak.AKTIVITETSVILKÅR_GENERELL_AVSLAGSÅRSAK, VilkårType.AKTIVITETSVILKÅR, Utfall.IKKE_OPPFYLT))
            : Set.<DetaljertVilkårResultat>of();
        var resultat = new DetaljertResultat(
            Set.of(BehandlingÅrsakType.ENDRET_AKTIVITET),
            avslåtteVilkår,
            Set.of(),
            Set.of(),
            UtbetalingsgradType.FULL,
            true
        );
        return DetaljertResultatTidslinje.av(new LocalDateTimeline<>(FOM, TOM, resultat));
    }

    private static VedtakSammenligningResultat vedtakSammenligningResultat(boolean uendret) {
        var vilkårEndring = uendret
            ? VilkårEndringType.UENDRET
            : VilkårEndringType.ENDRET;
        var dagsatsEndring = uendret
            ? DagsatsEndringType.UENDRET
            : DagsatsEndringType.REDUKSJON;
        return new VedtakSammenligningResultat(
            new LocalDateTimeline<>(FOM, TOM, Map.of(VilkårType.AKTIVITETSVILKÅR, vilkårEndring)),
            new LocalDateTimeline<>(FOM, TOM, dagsatsEndring)
        );
    }
}
