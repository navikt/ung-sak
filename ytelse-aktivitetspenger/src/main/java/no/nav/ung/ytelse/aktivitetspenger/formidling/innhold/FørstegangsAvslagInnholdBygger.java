package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.AvslagInngangsvilkårDto;

import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class FørstegangsAvslagInnholdBygger implements VedtaksbrevInnholdBygger {

    private final VilkårResultatRepository vilkårResultatRepository;

    @Inject
    public FørstegangsAvslagInnholdBygger(VilkårResultatRepository vilkårResultatRepository) {
        this.vilkårResultatRepository = vilkårResultatRepository;
    }

    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        LocalDateTimeline<DetaljertResultat> avslagPeriode = DetaljertResultat.filtererTidslinje(detaljertResultatTidslinje, DetaljertResultatType.AVSLAG_INNGANGSVILKÅR);
        var fom = avslagPeriode.getMinLocalDate();

        Set<DetaljertVilkårResultat> alleAvslåtteVilkår = AvslåttVilkårBrevinnholdHelper.hentAvslåtteVilkår(avslagPeriode);

        Set<VilkårType> avslåtteVilkårTyper = alleAvslåtteVilkår.stream()
            .map(DetaljertVilkårResultat::vilkårType)
            .collect(Collectors.toSet());

        var avslåttBosted = avslåtteVilkårTyper.contains(VilkårType.BOSTEDSVILKÅR) ?
            AvslåttVilkårBrevinnholdHelper.lagAvslåttBosted(alleAvslåtteVilkår, behandling, avslagPeriode, vilkårResultatRepository)
            : null;

        var avslåttBistand = avslåtteVilkårTyper.contains(VilkårType.BISTANDSVILKÅR) ?
            AvslåttVilkårBrevinnholdHelper.lagAvslåttBistand(alleAvslåtteVilkår, behandling, avslagPeriode, vilkårResultatRepository)
            : null;

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_AVSLAG_INNGANG,
            new AvslagInngangsvilkårDto(fom, avslåttBosted, avslåttBistand));
    }
}
