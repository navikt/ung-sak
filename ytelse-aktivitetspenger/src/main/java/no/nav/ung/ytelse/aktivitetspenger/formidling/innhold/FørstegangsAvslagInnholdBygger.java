package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBistand;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslåttBosted;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslagInngangsvilkårDto;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class FørstegangsAvslagInnholdBygger implements VedtaksbrevInnholdBygger {

    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FørstegangsAvslagInnholdBygger.class);

    private final VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public FørstegangsAvslagInnholdBygger(VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {

        LocalDateTimeline<DetaljertResultat> periode = DetaljertResultat.filtererTidslinje(detaljertResultatTidslinje, DetaljertResultatType.AVSLAG_INNGANGSVILKÅR);

        var fom = periode.getMinLocalDate();

        Set<DetaljertVilkårResultat> avslåtteVilkår = periode.stream()
            .flatMap(segment -> segment.getValue().avslåtteVilkår().stream())
            .collect(Collectors.toSet());

        AvslåttBosted avslåttBosted = null;
        AvslåttBistand avslåttBistand = null;

        boolean harBostedAvslag = avslåtteVilkår.stream()
            .anyMatch(v -> v.avslagsårsak() == Avslagsårsak.YTELSE_IKKE_TILGJENGELIG_PÅ_BOSTED);
        boolean harBistandAvslag = avslåtteVilkår.stream()
            .anyMatch(v -> v.avslagsårsak() == Avslagsårsak.IKKE_14A_VEDTAK);

        if (harBostedAvslag || harBistandAvslag) {
            Vilkårene vilkårene = vilkårResultatRepository.hent(behandling.getId());

            if (harBostedAvslag) {
                String fritekstBrev = hentFritekstBrev(vilkårene, VilkårType.BOSTEDSVILKÅR);
                avslåttBosted = new AvslåttBosted(true, fritekstBrev);
            }

            if (harBistandAvslag) {
                String fritekstBrev = hentFritekstBrev(vilkårene, VilkårType.BISTANDSVILKÅR);
                avslåttBistand = new AvslåttBistand(true, fritekstBrev);
            }
        }

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG,
            new AvslagInngangsvilkårDto(fom, avslåttBosted, avslåttBistand));
    }

    private String hentFritekstBrev(Vilkårene vilkårene, VilkårType vilkårType) {
        LocalDateTimeline<VilkårPeriode> timeline = vilkårene.getVilkårTimeline(vilkårType)
            .filterValue(vp -> vp.getGjeldendeUtfall() == Utfall.IKKE_OPPFYLT);
        return timeline.stream()
            .map(segment -> segment.getValue().getFritekstBrev())
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

}
