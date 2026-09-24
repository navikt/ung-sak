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
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultat;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslagInngangsvilkårDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.AvslåttVilkårBrevinnholdHjelper.VILKÅR_I_MALEN;

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
        Set<VilkårType> avslåtteVilkårTyper = tidslinje.tilVurdering().stream()
            .flatMap(s -> s.getValue().avslåtteVilkår().stream())
            .map(DetaljertVilkårResultat::vilkårType)
            .collect(Collectors.toSet());

        if (avslåtteVilkårTyper.stream().noneMatch(VILKÅR_I_MALEN::contains)) {
            throw new IllegalStateException("Avslag for vilkårtyper ikke implementert: " + avslåtteVilkårTyper + ", behandlingId: " + behandling.getId());
        }

        var vilkårVurdering = inngangsvilkårVurderingRepository.hentVurderingTidslinje(behandling.getId());

        Function<VilkårType, VilkårsvurderingResultat> vurderingFor = vilkårType ->
            hentVilkårsvurderingResultatPeriodeForVilkår(vilkårVurdering, tidslinje.avslåttPeriode(vilkårType), vilkårType, behandling);

        var bosted = avslåtteVilkårTyper.contains(VilkårType.BOSTEDSVILKÅR)
            ? AvslåttVilkårBrevinnholdHjelper.lagAvslåttBosted(vurderingFor.apply(VilkårType.BOSTEDSVILKÅR))
            : null;
        var bistand = avslåtteVilkårTyper.contains(VilkårType.BISTANDSVILKÅR)
            ? AvslåttVilkårBrevinnholdHjelper.lagAvslåttBistand(vurderingFor.apply(VilkårType.BISTANDSVILKÅR))
            : null;
        var andreLivsoppholdsytelser = avslåtteVilkårTyper.contains(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR)
            ? AvslåttVilkårBrevinnholdHjelper.lagAvslåttAndreLivsoppholdsytelser(vurderingFor.apply(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR))
            : null;

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG,
            new AvslagInngangsvilkårDto(bosted, bistand, andreLivsoppholdsytelser));
    }

    private static VilkårsvurderingResultat hentVilkårsvurderingResultatPeriodeForVilkår(LocalDateTimeline<Map<VilkårType, VilkårsvurderingResultat>> vilkårVurdering,
                                                                                        LocalDateTimeline<Boolean> avslagsperiodeForVilkår,
                                                                                        VilkårType vilkårType,
                                                                                        Behandling behandling) {
        var vilkårResultatPeriode = vilkårVurdering.intersection(avslagsperiodeForVilkår)
            .mapValue(it -> it.get(vilkårType))
            .segmenter().stream().map(LocalDateSegment::getValue)
            .distinct()
            .toList();

        if (vilkårResultatPeriode.isEmpty() || vilkårResultatPeriode.contains(null)) {
            throw new IllegalStateException("Mangler vilkårsvurdering for avslått vilkår " + vilkårType + ", behandlingId: " + behandling.getId());
        }
        if (vilkårResultatPeriode.size() > 1) {
            throw new IllegalStateException("Forventer kun en periode for vilkårstype " + vilkårType + ", men fant " + vilkårResultatPeriode.size());
        }

        return vilkårResultatPeriode.getFirst();
    }
}
