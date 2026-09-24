package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.vilkår.Avklaringtype;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringOgVurderingTidslinjeUtleder;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.AvslåttVilkårBrevinnholdHjelper;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.EndringAvslagInnholdBygger;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class EndringAvslagStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private EndringAvslagInnholdBygger endringAvslagInnholdBygger;
    private VilkårsavklaringOgVurderingTidslinjeUtleder vilkårsavklaringOgVurderingTidslinjeUtleder;

    public EndringAvslagStrategy() {
    }

    @Inject
    public EndringAvslagStrategy(EndringAvslagInnholdBygger endringAvslagInnholdBygger,
                                 VilkårsavklaringOgVurderingTidslinjeUtleder vilkårsavklaringOgVurderingTidslinjeUtleder) {
        this.endringAvslagInnholdBygger = endringAvslagInnholdBygger;
        this.vilkårsavklaringOgVurderingTidslinjeUtleder = vilkårsavklaringOgVurderingTidslinjeUtleder;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        var avklarteAvslag = AvslåttVilkårBrevinnholdHjelper.avklarteAvslag(
            vilkårsavklaringOgVurderingTidslinjeUtleder.utled(behandling.getId()), resultatTidslinje);

        if (avklarteAvslag.isEmpty()) {
            return List.of();
        }

        var harAvklaringMedOpphør = avklarteAvslag.values().stream()
            .flatMap(it -> it.stream())
            .anyMatch(it -> it.getValue().vilkårsavklaring().avklaringtype() == Avklaringtype.OPPHØR);

        return List.of(new VedtaksbrevStrategyResultat(
            harAvklaringMedOpphør ? DokumentMalType.OPPHØR_DOK : DokumentMalType.AVSLAG__DOK,
            endringAvslagInnholdBygger,
            VedtaksbrevEgenskaper.builder()
                .kanHindre(true)
                .kanOverstyreHindre(true)
                .kanRedigere(true)
                .kanOverstyreRediger(true)
                .build(),
            null,
            "Avslagsbrev ved revurdering med ikke oppfylte vilkår"
        ));
    }

}
