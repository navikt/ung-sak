package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.InngangsvilkårVurderingRepository;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslagInngangsvilkårDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        Vilkårene vilkårene = vilkårResultatRepository.hent(behandling.getId());
        var vurdertPeriode = avslagPeriode.mapValue(_ -> true);

        var vilkårVurdering = inngangsvilkårVurderingRepository.hentGrunnlag(behandling.getId())
            .orElseThrow(() -> new IllegalStateException("Fant ingen eksisterende vilkårvurderinggrunnlag for behandlingId: " + behandling.getId()));

        var avslåttBosted = avslåtteVilkårTyper.contains(VilkårType.BOSTEDSVILKÅR) ?
            AvslåttVilkårBrevinnholdHelper.lagAvslåttBosted(vilkårene, vilkårVurdering, vurdertPeriode)
            : null;

        var avslåttBistand = avslåtteVilkårTyper.contains(VilkårType.BISTANDSVILKÅR) ?
            AvslåttVilkårBrevinnholdHelper.lagAvslåttBistand(vilkårene, vilkårVurdering, vurdertPeriode)
            : null;

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG,
            new AvslagInngangsvilkårDto(fom, avslåttBosted, avslåttBistand));
    }
}
