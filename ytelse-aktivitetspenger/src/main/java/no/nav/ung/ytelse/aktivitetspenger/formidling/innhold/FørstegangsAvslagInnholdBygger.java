package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslagInngangsvilkårDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class FørstegangsAvslagInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = LoggerFactory.getLogger(FørstegangsAvslagInnholdBygger.class);

    private final VilkårResultatRepository vilkårResultatRepository;
    private final InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository;

    @Inject
    public FørstegangsAvslagInnholdBygger(VilkårResultatRepository vilkårResultatRepository,
                                          InngangsvilkårVurderingRepository inngangsvilkårVurderingRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
        this.inngangsvilkårVurderingRepository = inngangsvilkårVurderingRepository;
    }

    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, DetaljertResultatTidslinje tidslinje) {
        var detaljertResultatTidslinje = tidslinje.tilVurdering();
        LocalDateTimeline<DetaljertResultat> avslagPeriode = detaljertResultatTidslinje
            .filterValue(r -> !r.avslåtteVilkår().isEmpty());
        var fom = avslagPeriode.getMinLocalDate();

        Set<DetaljertVilkårResultat> alleAvslåtteVilkår = avslagPeriode.stream()
            .flatMap(s -> s.getValue().avslåtteVilkår().stream())
            .collect(Collectors.toSet());

        Set<VilkårType> avslåtteVilkårTyper = alleAvslåtteVilkår.stream()
            .map(DetaljertVilkårResultat::vilkårType)
            .collect(Collectors.toSet());

        var vurdertPeriode = avslagPeriode.mapValue(_ -> true);

        var vilkårVurdering = inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandling.getId());

        var avslåttBosted = avslåtteVilkårTyper.contains(VilkårType.BOSTEDSVILKÅR) ?
            AvslåttVilkårBrevinnholdHjelper.lagAvslåttBosted(
                hentVilkårsvurderingResultatPeriodeForVilkår(vilkårVurdering, vurdertPeriode, VilkårType.BOSTEDSVILKÅR)
            ) : null;

        var avslåttBistand = avslåtteVilkårTyper.contains(VilkårType.BISTANDSVILKÅR) ?
            AvslåttVilkårBrevinnholdHjelper.lagAvslåttBistand(
                hentVilkårsvurderingResultatPeriodeForVilkår(vilkårVurdering, vurdertPeriode, VilkårType.BISTANDSVILKÅR)
            ) : null;

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG,
            new AvslagInngangsvilkårDto(fom, avslåttBosted, avslåttBistand));
    }

    private static VilkårsvurderingResultat hentVilkårsvurderingResultatPeriodeForVilkår(LocalDateTimeline<Map<VilkårType, VilkårsvurderingResultat>> vilkårVurdering, LocalDateTimeline<Boolean> vurdertPeriode, VilkårType vilkårType) {
        var vilkårResultatPeriode = vilkårVurdering.intersection(vurdertPeriode)
            .mapValue(it -> it.get(vilkårType))
            .segmenter().stream().map(LocalDateSegment::getValue)
            .distinct()
            .toList();

        if (vilkårResultatPeriode.size() > 1) {
            throw new IllegalStateException("Forventer kun en periode for vilkårstype " + vilkårType + ", men fant " + vilkårResultatPeriode.size());
        }

        return vilkårResultatPeriode.getFirst();
    }
}
