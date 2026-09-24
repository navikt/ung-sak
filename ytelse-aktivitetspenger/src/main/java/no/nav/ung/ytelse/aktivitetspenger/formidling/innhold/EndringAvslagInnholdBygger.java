package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringMedVurdering;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringOgVurderingTidslinjeUtleder;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.EndringAvslagDto;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.KildeTilOpplysninger;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Dependent
public class EndringAvslagInnholdBygger implements VedtaksbrevInnholdBygger {

    static final Set<VilkårType> VILKÅR_I_MALEN = EnumSet.of(
        VilkårType.BOSTEDSVILKÅR,
        VilkårType.BISTANDSVILKÅR,
        VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR);

    private final VilkårsavklaringOgVurderingTidslinjeUtleder vilkårsavklaringOgVurderingTidslinjeUtleder;

    @Inject
    public EndringAvslagInnholdBygger(VilkårsavklaringOgVurderingTidslinjeUtleder vilkårsavklaringOgVurderingTidslinjeUtleder) {
        this.vilkårsavklaringOgVurderingTidslinjeUtleder = vilkårsavklaringOgVurderingTidslinjeUtleder;
    }

    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, DetaljertResultatTidslinje tidslinje) {
        Set<VilkårType> avslåtteVilkårTyper = tidslinje.tilVurdering()
            .stream()
            .flatMap(s -> s.getValue().avslåtteVilkår().stream())
            .filter(AvslåttVilkårBrevinnholdHjelper::erFunksjoneltAvslag)
            .map(DetaljertVilkårResultat::vilkårType)
            .collect(Collectors.toSet());

        var vilkårIBrevet = avslåtteVilkårTyper.stream().filter(VILKÅR_I_MALEN::contains).toList();
        if (vilkårIBrevet.isEmpty()) {
            throw new IllegalStateException("Avslag for vilkårtyper ikke implementert: " + avslåtteVilkårTyper + ", behandlingId: " + behandling.getId());
        }

        var avklartOgVurdertTidslinje = vilkårsavklaringOgVurderingTidslinjeUtleder.utled(behandling.getId());

        // Et vilkår kan være avslått uten at det er avklart i denne behandlingen - da har brevet ingenting å si om det.
        Map<VilkårType, LocalDateSegment<VilkårsavklaringMedVurdering>> avslåtteSegmenter = new EnumMap<>(VilkårType.class);
        for (var vilkårType : vilkårIBrevet) {
            førsteAvslåtteSegment(tidslinje, avklartOgVurdertTidslinje, vilkårType)
                .ifPresent(segment -> avslåtteSegmenter.put(vilkårType, segment));
        }
        if (avslåtteSegmenter.isEmpty()) {
            throw new IllegalStateException("Fant ingen vilkårsavklaring med avslått periode for behandlingId: " + behandling.getId());
        }

        var avklaringer = avslåtteSegmenter.values().stream().map(LocalDateSegment::getValue).toList();
        var avklaringstyper = distinkt(avklaringer, it -> it.vilkårsavklaring().avklaringtype());
        var perioder = distinkt(avslåtteSegmenter.values(), EndringAvslagInnholdBygger::periodeFor);
        var kilder = distinkt(avklaringer, EndringAvslagInnholdBygger::kildeFor);

        if (avklaringstyper.size() > 1 || perioder.size() > 1 || kilder.size() > 1) {
            throw new IllegalStateException("Vedtaksbrev kan ikke omtale vilkår som er avklart ulikt"
                + " - perioder: " + perioder + ", avklaringstyper: " + avklaringstyper + ", kilder: " + kilder
                + ", behandlingId: " + behandling.getId());
        }

        var bosted = AvslåttVilkårBrevinnholdHjelper.lagAvslåttBosted(vurderingFor(avslåtteSegmenter, VilkårType.BOSTEDSVILKÅR));
        var bistand = AvslåttVilkårBrevinnholdHjelper.lagAvslåttBistand(vurderingFor(avslåtteSegmenter, VilkårType.BISTANDSVILKÅR));
        var andreLivsoppholdsytelser = AvslåttVilkårBrevinnholdHjelper.lagAvslåttAndreLivsoppholdsytelser(
            vurderingFor(avslåtteSegmenter, VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR));

        return new TemplateInnholdResultat(
            avklaringstyper.getFirst() == Avklaringtype.OPPHØR ? TemplateType.AKTIVITETSPENGER_OPPHØR : TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG,
            new EndringAvslagDto(perioder.getFirst(), kilder.getFirst(), bosted, bistand, andreLivsoppholdsytelser));
    }

    private static <T, V> List<V> distinkt(Collection<T> elementer, Function<T, V> egenskap) {
        return elementer.stream().map(egenskap).distinct().toList();
    }

    private static VilkårsvurderingResultat vurderingFor(Map<VilkårType, LocalDateSegment<VilkårsavklaringMedVurdering>> avslåtteSegmenter,
                                                         VilkårType vilkårType) {
        var segment = avslåtteSegmenter.get(vilkårType);
        return segment == null ? null : segment.getValue().vilkårsvurdering();
    }

    private static Periode periodeFor(LocalDateSegment<VilkårsavklaringMedVurdering> segment) {
        return new Periode(segment.getFom(), segment.getTom());
    }

    private static KildeTilOpplysninger kildeFor(VilkårsavklaringMedVurdering avklaringMedVurdering) {
        var avklaring = avklaringMedVurdering.vilkårsavklaring();
        return KildeTilOpplysninger.av(avklaring.kilde(), avklaring.kildeFritekst());
    }

    private static Optional<LocalDateSegment<VilkårsavklaringMedVurdering>> førsteAvslåtteSegment(DetaljertResultatTidslinje resultattidslinje,
                                                                                                   LocalDateTimeline<Map<VilkårType, VilkårsavklaringMedVurdering>> avklartOgVurdertTidslinje,
                                                                                                   VilkårType vilkårType) {
        var avslåttPeriode = avklartOgVurdertTidslinje
            .mapValue(it -> it.get(vilkårType))
            .filterValue(it -> it != null && it.harVilkårsAvklaring())
            .intersection(avslåttVilkårsPeriode(resultattidslinje, vilkårType));

        if (avslåttPeriode.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(avslåttPeriode.segmenter().getFirst());
    }

    private static LocalDateTimeline<Boolean> avslåttVilkårsPeriode(DetaljertResultatTidslinje tidslinje, VilkårType vilkårType) {
        return tidslinje.tilVurdering()
            .filterValue(r -> r.avslåtteVilkår().stream()
                .anyMatch(v -> v.vilkårType() == vilkårType && AvslåttVilkårBrevinnholdHjelper.erFunksjoneltAvslag(v)))
            .mapValue(_ -> Boolean.TRUE);
    }
}
