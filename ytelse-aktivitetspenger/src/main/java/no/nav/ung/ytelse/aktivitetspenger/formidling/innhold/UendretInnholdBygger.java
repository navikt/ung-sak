package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultatPeriode;
import no.nav.ung.sak.domene.typer.tid.TidslinjeUtil;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringMedVurdering;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringOgVurderingTidslinjeUtleder;
import no.nav.ung.sak.inngangsvilkår.avklaring.Vilkårsavklaring;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.UendretDto;

import java.util.Objects;

@Dependent
public class UendretInnholdBygger implements VedtaksbrevInnholdBygger {

    private final VilkårsavklaringOgVurderingTidslinjeUtleder vilkårsavklaringOgVurderingTidslinjeUtleder;

    @Inject
    public UendretInnholdBygger(VilkårsavklaringOgVurderingTidslinjeUtleder vilkårsavklaringOgVurderingTidslinjeUtleder) {
        this.vilkårsavklaringOgVurderingTidslinjeUtleder = vilkårsavklaringOgVurderingTidslinjeUtleder;
    }

    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, DetaljertResultatTidslinje tidslinje) {
        var avklartOgVurdert = vilkårsavklaringOgVurderingTidslinjeUtleder.utled(behandling.getId());

        var periodeFremdelesInnvilget = avklartOgVurdert.disjoint(avslåttVilkårsPeriode(tidslinje));
        if (periodeFremdelesInnvilget.isEmpty()) {
            throw new IllegalStateException("Fant ingen vilkårsavklaring uten avslått periode for behandlingId: " + behandling.getId());
        }

        var vilkårsavklaringer = TidslinjeUtil.values(periodeFremdelesInnvilget).stream()
            .map(VilkårsavklaringMedVurdering::vilkårsavklaring)
            .distinct()
            .toList();

        if (vilkårsavklaringer.size() != 1) {
            throw new IllegalStateException("Vedtaksbrev ved uendret vedtak forventer kun én vilkårsavklaring for behandlingId: " + behandling.getId());
        }

        var fritekst = TidslinjeUtil.values(periodeFremdelesInnvilget).stream()
            .map(VilkårsavklaringMedVurdering::vilkårsvurdering)
            .filter(Objects::nonNull)
            .map(VilkårsvurderingResultatPeriode::getFritekstVurderingBrev)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);

        return lagDto(vilkårsavklaringer.getFirst(), fritekst);
    }

    private static LocalDateTimeline<Boolean> avslåttVilkårsPeriode(DetaljertResultatTidslinje tidslinje) {
        return tidslinje.tilVurdering()
            .filterValue(r -> !r.avslåtteVilkår().isEmpty())
            .mapValue(_ -> Boolean.TRUE);
    }

    public TemplateInnholdResultat lagDto(Vilkårsavklaring vilkårsavklaring, String fritekst) {
        return new TemplateInnholdResultat(
            TemplateType.AKTIVITETSPENGER_UENDRET,
            new UendretDto(
                vilkårsavklaring.avklaringtype() == Avklaringtype.OPPHØR,
                vilkårsavklaring.periode().tilPeriode(),
                fritekst
            )
        );
    }
}
