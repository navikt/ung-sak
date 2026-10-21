package no.nav.ung.ytelse.aktivitetspenger.formidling.innhold;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.Dependent;
import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TemplateInnholdResultat;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.ytelse.aktivitetspenger.formidling.dto.UendretDto;

@Dependent
public class UendretInnholdBygger implements VedtaksbrevInnholdBygger {

    @WithSpan
    @Override
    public TemplateInnholdResultat bygg(Behandling behandling, DetaljertResultatTidslinje tidslinje) {
        var innvilget = tidslinje.totalTidslinje()
            .filterValue(it -> !it.erAvslåttEllerAvkortet())
            .mapValue(_ -> Boolean.TRUE)
            .compress();

        var innvilgetTilVurdering = innvilget.intersection(tidslinje.tilVurdering());
        if (innvilgetTilVurdering.isEmpty()) {
            throw new IllegalStateException("Fant ingen innvilget periode til vurdering for behandlingId: " + behandling.getId());
        }

        // Maksdato er siste dag i den sammenhengende innvilgede perioden, også når den strekker seg forbi periodene til vurdering.
        var maksdato = innvilget
            .intersection(new LocalDateInterval(innvilgetTilVurdering.getMaxLocalDate(), LocalDateInterval.TIDENES_ENDE))
            .segmenter().getFirst()
            .getTom();

        return new TemplateInnholdResultat(TemplateType.AKTIVITETSPENGER_UENDRET, new UendretDto(maksdato));
    }
}
