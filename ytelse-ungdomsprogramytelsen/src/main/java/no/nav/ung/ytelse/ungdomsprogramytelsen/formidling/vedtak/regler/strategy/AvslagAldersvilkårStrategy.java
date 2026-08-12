package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.innhold.TomVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.regler.VedtaksbrevEgenskaper;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.Presedens;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinje;

import java.util.List;

/**
 * Avslag på ungdomsprogramvilkåret håndteres av {@link ProgramPeriodeStrategy}, så aldersvilkåret er det eneste
 * som gir avslagsbrev her. Andre avslag faller til IKKE_IMPLEMENTERT og må vurderes når de dukker opp.
 */
@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class AvslagAldersvilkårStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final TomVedtaksbrevInnholdBygger tomVedtaksbrevInnholdBygger;

    @Inject
    public AvslagAldersvilkårStrategy(TomVedtaksbrevInnholdBygger tomVedtaksbrevInnholdBygger) {
        this.tomVedtaksbrevInnholdBygger = tomVedtaksbrevInnholdBygger;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, DetaljertResultatTidslinje resultatTidslinje) {
        if (resultatTidslinje.harKunAvslåttVilkår(VilkårType.ALDERSVILKÅR)) {
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
                "Tomt brev for redigering ved avslag på aldersvilkåret"
            ));
        }

        return List.of();
    }

    @Override
    public Presedens presedens() {
        return Presedens.OVERSTYRENDE_ENKELTBREV;
    }
}
