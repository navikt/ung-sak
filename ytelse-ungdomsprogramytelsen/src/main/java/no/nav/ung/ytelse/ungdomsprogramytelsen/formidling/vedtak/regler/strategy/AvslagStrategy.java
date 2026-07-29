package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TomVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.Presedens;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class AvslagStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final TomVedtaksbrevInnholdBygger tomVedtaksbrevInnholdBygger;

    @Inject
    public AvslagStrategy(TomVedtaksbrevInnholdBygger tomVedtaksbrevInnholdBygger) {
        this.tomVedtaksbrevInnholdBygger = tomVedtaksbrevInnholdBygger;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        if (erHeleBehandlingenAvslag(detaljertResultat)) {
            return List.of(new VedtaksbrevStrategyResultat(
                DokumentMalType.MANUELT_VEDTAK_DOK,
                tomVedtaksbrevInnholdBygger,
                VedtaksbrevEgenskaper.builder()
                    .kanHindre(true)
                    .kanOverstyreHindre(true)
                    .kanRedigere(true)
                    .kanOverstyreRediger(true)
                    .build(),
                null,
                "Tomt brev for redigering ved avslag"
            ));
        }

        return List.of();
    }

    // Fullt avslag = hele vilkårstidslinjen er avslått. Ved opphør/kombinasjoner finnes det oppfylte perioder
    // (før sluttdato), og da eier ProgramPeriodeStrategy/andre strategier resultatet.
    private static boolean erHeleBehandlingenAvslag(LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return !detaljertResultat.isEmpty()
            && detaljertResultat.stream().noneMatch(it -> it.getValue().avslåtteVilkår().isEmpty());
    }

    @Override
    public Presedens presedens() {
        return Presedens.OVERSTYRENDE_ENKELTBREV;
    }
}



