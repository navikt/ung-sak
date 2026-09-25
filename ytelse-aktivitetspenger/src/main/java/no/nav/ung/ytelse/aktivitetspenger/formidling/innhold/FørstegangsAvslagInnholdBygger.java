package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslagInngangsvilkårDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@Dependent
public class FørstegangsAvslagInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = LoggerFactory.getLogger(FørstegangsAvslagInnholdBygger.class);

    private final InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;

    @Inject
    public FørstegangsAvslagInnholdBygger(VilkårResultatRepository vilkårResultatRepository,
                                          InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository) {
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
    }

    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, DetaljertResultatTidslinje tidslinje) {
        LocalDateTimeline<DetaljertResultat> avslåttTidslinje = tidslinje.tilVurdering()
            .filterValue(r -> !r.avslåtteVilkår().isEmpty());
        var fom = avslåttTidslinje.getMinLocalDate();

        var vilkårVurdering = inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandling.getId());

        var avslåttBostedResultat = hentAvslåttVilkårsvurderingResultat(avslåttTidslinje, vilkårVurdering, VilkårType.BOSTEDSVILKÅR);
        var avslåttBosted = avslåttBostedResultat != null ? AvslåttVilkårBrevinnholdHjelper.lagAvslåttBosted(avslåttBostedResultat) : null;

        var avslåttBistandResultat = hentAvslåttVilkårsvurderingResultat(avslåttTidslinje, vilkårVurdering, VilkårType.BISTANDSVILKÅR);
        var avslåttBistand = avslåttBistandResultat != null ? AvslåttVilkårBrevinnholdHjelper.lagAvslåttBistand(avslåttBistandResultat) : null;

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG,
            new AvslagInngangsvilkårDto(fom, avslåttBosted, avslåttBistand));
    }

    private static VilkårsvurderingResultat hentAvslåttVilkårsvurderingResultat(LocalDateTimeline<DetaljertResultat> avslåttTidslinje,
                                                                                 LocalDateTimeline<Map<VilkårType, VilkårsvurderingResultat>> vilkårVurdering,
                                                                                 VilkårType vilkårType) {
        var avslåttVilkårTidslinje = avslåttTidslinje.filterValue(r -> r.avslåtteVilkår().stream()
            .anyMatch(v -> v.vilkårType().equals(vilkårType) && v.avslagsårsak() != Avslagsårsak.AVKORTET));

        var vilkårResultatPeriode = vilkårVurdering.intersection(avslåttVilkårTidslinje)
            .mapValue(it -> it.get(vilkårType))
            .segmenter().stream().map(LocalDateSegment::getValue)
            .distinct()
            .filter(it -> !it.godkjent())
            .toList();

        if (vilkårResultatPeriode.size() > 1) {
            throw new IllegalStateException("Forventer kun en periode avslått for vilkårstype " + vilkårType + ", men fant " + vilkårResultatPeriode.size());
        }

        return vilkårResultatPeriode.isEmpty() ? null : vilkårResultatPeriode.getFirst();
    }
}
