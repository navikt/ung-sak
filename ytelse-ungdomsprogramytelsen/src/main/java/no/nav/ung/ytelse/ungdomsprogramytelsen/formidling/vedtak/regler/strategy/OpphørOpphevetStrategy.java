package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramOpphørUtleder;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.vedtak.regler.IngenBrevÅrsakType;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.BehandlingÅrsakHelper;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.OpphørOpphevetInnholdBygger;

import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class OpphørOpphevetStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final OpphørOpphevetInnholdBygger opphørOpphevetInnholdBygger;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public OpphørOpphevetStrategy(
        OpphørOpphevetInnholdBygger opphørOpphevetInnholdBygger,
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.opphørOpphevetInnholdBygger = opphørOpphevetInnholdBygger;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        if (!new BehandlingÅrsakHelper(detaljertResultat).har(BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM)) {
            return List.of();
        }
        if (UngdomsprogramOpphørUtleder.opphørAvUngdomsprogrammetVarInkludertIVedtaket(behandling, ungdomsprogramPeriodeRepository)) {
            return List.of(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.OPPHOR_OPPHEVET_DOK, opphørOpphevetInnholdBygger,
                "Automatisk brev ved opphevelse av opphør."));
        }
        // Opphør og opphevelse havnet på samme, fortsatt åpne behandling: opphøret ble aldri vedtatt, så det er ikke
        // noe å oppheve. Returnerer et eksplisitt ingen-brev-resultat (ikke tom liste, som ville gitt IKKE_IMPLEMENTERT
        // og krevd manuell «Fatt vedtak» dersom heller ingen annen strategi ga brev).
        return List.of(VedtaksbrevStrategyResultat.utenBrev(IngenBrevÅrsakType.IKKE_RELEVANT,
            "Opphør og opphevelse havnet på samme, fortsatt åpne behandling - opphøret ble aldri vedtatt, ikke behov for vedtaksbrev."));
    }

}

