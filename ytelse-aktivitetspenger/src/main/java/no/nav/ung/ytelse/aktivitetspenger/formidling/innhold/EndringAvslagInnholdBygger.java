package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;
import no.nav.ung.sak.inngangsvilkår.avklaring.Vilkårsavklaring;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringMedVurdering;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringOgVurderingTidslinjeUtleder;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.EndringAvslagDto;

import java.util.Set;
import java.util.stream.Collectors;

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
        Set<VilkårType> avslåtteVilkårTyper = tidslinje.tilVurdering()
            .filterValue(r -> !r.avslåtteVilkår().isEmpty())
            .stream()
            .flatMap(s -> s.getValue().avslåtteVilkår().stream())
            .map(DetaljertVilkårResultat::vilkårType)
            .collect(Collectors.toSet());

        var avklartOgVurdertTidslinje = vilkårsavklaringOgVurderingTidslinjeUtleder.utled(behandling.getId());

        if (avslåtteVilkårTyper.contains(VilkårType.BOSTEDSVILKÅR)) {
            var avslåttPeriode = avklartOgVurdertTidslinje
                .filterValue(it -> it.vilkårType() == VilkårType.BOSTEDSVILKÅR)
                .filterValue(VilkårsavklaringMedVurdering::harVilkårsAvklaring)
                .intersection(avslåttVilkårsPeriode(tidslinje, VilkårType.BOSTEDSVILKÅR));

            if (avslåttPeriode.isEmpty()) {
                throw new IllegalStateException("Fant ingen vilkårsavklaring med avslått periode for behandlingId: " + behandling.getId());
            }

            var vilkårsavklaringOgVurdering = avslåttPeriode.segmenter().getFirst();
            return lagDto(
                vilkårsavklaringOgVurdering.getLocalDateInterval(),
                vilkårsavklaringOgVurdering.getValue().vilkårsavklaring(),
                 AvslåttVilkårBrevinnholdHjelper.lagAvslåttBosted(vilkårsavklaringOgVurdering.getValue().vilkårsvurdering())
            );
        }
        throw new IllegalStateException("Avslag for vilkårType ikke implementert: " + behandling.getId());
    }

    private static LocalDateTimeline<Boolean> avslåttVilkårsPeriode(DetaljertResultatTidslinje tidslinje, VilkårType vilkårType) {
        return tidslinje.tilVurdering()
            .filterValue(r -> r.avslåtteVilkår().stream().anyMatch(v -> v.vilkårType() == vilkårType))
            .mapValue(_ -> Boolean.TRUE);
    }

    public TemplateInnholdResultat lagDto(LocalDateInterval localDateInterval, Vilkårsavklaring vilkårsavklaring, AvslåttBosted avslåttBosted) {
        return new TemplateInnholdResultat(
            vilkårsavklaring.avklaringtype() == Avklaringtype.OPPHØR ? TemplateType.AKTIVITETSPENGER_OPPHØR : TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG,
            new EndringAvslagDto(
                new Periode(localDateInterval.getFomDato(), localDateInterval.getTomDato()),
                avslåttBosted
            )
        );
    }
}
