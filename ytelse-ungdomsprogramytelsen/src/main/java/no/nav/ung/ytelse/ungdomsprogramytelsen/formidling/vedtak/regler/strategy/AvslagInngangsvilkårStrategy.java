package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
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
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class AvslagInngangsvilkårStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final TomVedtaksbrevInnholdBygger tomVedtaksbrevInnholdBygger;

    @Inject
    public AvslagInngangsvilkårStrategy(TomVedtaksbrevInnholdBygger tomVedtaksbrevInnholdBygger) {
        this.tomVedtaksbrevInnholdBygger = tomVedtaksbrevInnholdBygger;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        if (erAvslagPåNySøktPeriode(resultatTidslinje.tilVurdering())) {
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


    private static boolean erAvslagPåNySøktPeriode(LocalDateTimeline<DetaljertResultat> tilVurdering) {
        return !tilVurdering.isEmpty()
            && tilVurdering.stream().allMatch(it -> erAvslag(it.getValue()));
    }

    private static boolean erAvslag(DetaljertResultat r) {
        return r.harÅrsak(BehandlingÅrsakType.NY_SØKT_PERIODE)
            && !r.avslåtteVilkår().isEmpty()
            && !r.utbetalingsgrad().erSatt();
    }

    @Override
    public Presedens presedens() {
        return Presedens.OVERSTYRENDE_ENKELTBREV;
    }
}



