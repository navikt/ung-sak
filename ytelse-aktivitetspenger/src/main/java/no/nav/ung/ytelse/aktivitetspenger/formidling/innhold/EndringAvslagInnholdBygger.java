package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringOgVurderingTidslinjeUtleder;
import no.nav.ung.sak.typer.Periode;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.EndringAvslagDto;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

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
        Map<VilkårType, AvslåttVurdering> avslag = new EnumMap<>(VilkårType.class);
        Set<Avklaringtype> avklaringstyper = EnumSet.noneOf(Avklaringtype.class);
        for (var entry : avklarteAvslag.entrySet()) {
            var vilkårType = entry.getKey();
            if (!VILKÅR_I_MALEN.contains(vilkårType)) {
                continue;
            }
            var segment = entry.getValue().segmenter().getFirst();
            var avklaring = segment.getValue();
            if (avklaring.vilkårsvurdering() == null) {
                throw new IllegalStateException("Mangler vilkårsvurdering for avslått vilkår " + vilkårType + ", behandlingId: " + behandling.getId());
            }
            avklaringstyper.add(avklaring.vilkårsavklaring().avklaringtype());
            avslag.put(vilkårType, new AvslåttVurdering(avklaring.vilkårsvurdering(), new Periode(segment.getFom(), segment.getTom())));
        }

        if (avslag.isEmpty()) {
            throw new IllegalStateException("Fant ingen vilkårsavklaring med avslått periode for vilkår i malen, avklarte avslag: "
                + avklarteAvslag.keySet() + ", behandlingId: " + behandling.getId());
        }
        if (avklaringstyper.size() > 1) {
            throw new IllegalStateException("Vedtaksbrev kan ikke omtale vilkår som er avklart ulikt"
                + " - avklaringstyper: " + avklaringstyper + ", behandlingId: " + behandling.getId());
        }

        var dto = new EndringAvslagDto(
            AvslåttVilkårBrevinnholdHjelper.lagAvslåttBosted(avslag.get(VilkårType.BOSTEDSVILKÅR)),
            AvslåttVilkårBrevinnholdHjelper.lagAvslåttBistand(avslag.get(VilkårType.BISTANDSVILKÅR)),
            AvslåttVilkårBrevinnholdHjelper.lagAvslåttPgaAndreLivsoppholdsytelser(avslag.get(VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR)));

        var templateType = avklaringstyper.contains(Avklaringtype.OPPHØR)
            ? TemplateType.AKTIVITETSPENGER_OPPHØR
            : TemplateType.AKTIVITETSPENGER_ENDRING_AVSLAG;
        return new TemplateInnholdResultat(templateType, dto);
    }
}
