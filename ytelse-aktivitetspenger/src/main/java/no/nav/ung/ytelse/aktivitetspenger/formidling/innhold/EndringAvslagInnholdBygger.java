package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringMedVurdering;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringOgVurderingTidslinjeUtleder;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.EndringAvslagDto;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.AvslåttVilkårBrevinnholdHjelper.VILKÅR_I_MALEN;

@Dependent
public class EndringAvslagInnholdBygger implements VedtaksbrevInnholdBygger {

    private final VilkårsavklaringOgVurderingTidslinjeUtleder vilkårsavklaringOgVurderingTidslinjeUtleder;

    @Inject
    public EndringAvslagInnholdBygger(VilkårsavklaringOgVurderingTidslinjeUtleder vilkårsavklaringOgVurderingTidslinjeUtleder) {
        this.vilkårsavklaringOgVurderingTidslinjeUtleder = vilkårsavklaringOgVurderingTidslinjeUtleder;
    }

    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, DetaljertResultatTidslinje tidslinje) {
        var avklarteAvslag = AvslåttVilkårBrevinnholdHjelper.avklarteAvslag(
            vilkårsavklaringOgVurderingTidslinjeUtleder.utled(behandling.getId()), tidslinje);

        // Et vilkår kan være avslått uten at det er avklart i denne behandlingen - da har brevet ingenting å si om det.
        Map<VilkårType, LocalDateSegment<VilkårsavklaringMedVurdering>> avslåtteSegmenter = new EnumMap<>(VilkårType.class);
        avklarteAvslag.forEach((vilkårType, perioder) -> {
            if (VILKÅR_I_MALEN.contains(vilkårType)) {
                avslåtteSegmenter.put(vilkårType, perioder.segmenter().getFirst());
            }
        });
        if (avslåtteSegmenter.isEmpty()) {
            throw new IllegalStateException("Fant ingen vilkårsavklaring med avslått periode for vilkår i malen, avklarte avslag: "
                + avklarteAvslag.keySet() + ", behandlingId: " + behandling.getId());
        }

        var avklaringer = avslåtteSegmenter.values().stream().map(LocalDateSegment::getValue).toList();
        var avklaringstyper = distinkt(avklaringer, it -> it.vilkårsavklaring().avklaringtype());
        var perioder = distinkt(avslåtteSegmenter.values(), EndringAvslagInnholdBygger::periodeFor);

        if (avklaringstyper.size() > 1 || perioder.size() > 1) {
            throw new IllegalStateException("Vedtaksbrev kan ikke omtale vilkår som er avklart ulikt"
                + " - perioder: " + perioder + ", avklaringstyper: " + avklaringstyper
                + ", behandlingId: " + behandling.getId());
        }

        var bosted = avslåtteSegmenter.containsKey(VilkårType.BOSTEDSVILKÅR)
            ? AvslåttVilkårBrevinnholdHjelper.lagAvslåttBosted(vurderingFor(avslåtteSegmenter, VilkårType.BOSTEDSVILKÅR, behandling))
            : null;
        var bistand = avslåtteSegmenter.containsKey(VilkårType.BISTANDSVILKÅR)
            ? AvslåttVilkårBrevinnholdHjelper.lagAvslåttBistand(vurderingFor(avslåtteSegmenter, VilkårType.BISTANDSVILKÅR, behandling))
            : null;
        var andreLivsoppholdsytelser = avslåtteSegmenter.containsKey(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR)
            ? AvslåttVilkårBrevinnholdHjelper.lagAvslåttPgaAndreLivsoppholdsytelser(
                vurderingFor(avslåtteSegmenter, VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, behandling))
            : null;

        return new TemplateInnholdResultat(
            avklaringstyper.getFirst() == Avklaringtype.OPPHØR ? TemplateType.AKTIVITETSPENGER_OPPHØR : TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG,
            new EndringAvslagDto(perioder.getFirst(), bosted, bistand, andreLivsoppholdsytelser));
    }

    private static <T, V> List<V> distinkt(Collection<T> elementer, Function<T, V> egenskap) {
        return elementer.stream().map(egenskap).distinct().toList();
    }

    private static VilkårsvurderingResultat vurderingFor(Map<VilkårType, LocalDateSegment<VilkårsavklaringMedVurdering>> avslåtteSegmenter,
                                                         VilkårType vilkårType,
                                                         Behandling behandling) {
        var vurdering = avslåtteSegmenter.get(vilkårType).getValue().vilkårsvurdering();
        if (vurdering == null) {
            throw new IllegalStateException("Mangler vilkårsvurdering for avslått vilkår " + vilkårType + ", behandlingId: " + behandling.getId());
        }
        return vurdering;
    }

    private static Periode periodeFor(LocalDateSegment<VilkårsavklaringMedVurdering> segment) {
        return new Periode(segment.getFom(), segment.getTom());
    }
}
