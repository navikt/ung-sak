package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.formidling.vedtak.resultat.VedtakEndringSammenligner;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringMedVurdering;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringOgVurderingTidslinjeUtleder;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.UendretInnholdBygger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class UendretStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(UendretStrategy.class);

    private UendretInnholdBygger uendretInnholdBygger;
    private VilkårsavklaringOgVurderingTidslinjeUtleder vilkårsavklaringOgVurderingTidslinjeUtleder;
    private VedtakEndringSammenligner vedtakEndringSammenligner;

    public UendretStrategy() {
    }

    @Inject
    public UendretStrategy(UendretInnholdBygger uendretInnholdBygger,
                           VilkårsavklaringOgVurderingTidslinjeUtleder vilkårsavklaringOgVurderingTidslinjeUtleder,
                           VedtakEndringSammenligner vedtakEndringSammenligner) {
        this.uendretInnholdBygger = uendretInnholdBygger;
        this.vilkårsavklaringOgVurderingTidslinjeUtleder = vilkårsavklaringOgVurderingTidslinjeUtleder;
        this.vedtakEndringSammenligner = vedtakEndringSammenligner;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        var tilVurdering = resultatTidslinje.tilVurdering();
        if (!harVilkårsavklaring(behandling, tilVurdering)) {
            return List.of();
        }

        // Avslag skal ikke beskrives med brev for uendret vedtak, men alltid bruke mal for avslag.
        var harAvslåtteVilkår = tilVurdering.stream().anyMatch(it -> !it.getValue().avslåtteVilkår().isEmpty());
        if (harAvslåtteVilkår) {
            return List.of();
        }

        var endring = vedtakEndringSammenligner.sammenlignMedOriginal(behandling, tilVurdering);
        if (endring.isEmpty()) {
            return List.of();
        }
        if (!endring.get().erUendret()) {
            LOG.info("Hopper over strategi for uendret vedtak da vedtaket er endret for behandling {}", behandling.getId());
            return List.of();
        }

        return List.of(new VedtaksbrevStrategyResultat(
            DokumentMalType.INGEN_ENDRING,
            uendretInnholdBygger,
            VedtaksbrevEgenskaper.builder()
                .kanHindre(true)
                .kanOverstyreHindre(true)
                .kanRedigere(true)
                .kanOverstyreRediger(true)
                .build(),
            null,
            "Revurdering med forslag til avslag/opphør uten endringer"
        ));
    }

    private boolean harVilkårsavklaring(Behandling behandling, LocalDateTimeline<DetaljertResultat> tilVurdering) {
        return vilkårsavklaringOgVurderingTidslinjeUtleder.utled(behandling.getId())
            .intersection(tilVurdering)
            .stream()
            .anyMatch(segment -> segment.getValue().values().stream().anyMatch(VilkårsavklaringMedVurdering::harVilkårsAvklaring));
    }
}
