package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.OpphørOpphevetUtleder;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.OpphørOpphevetInnholdBygger;

import java.util.List;

/**
 * Vedtaksbrev for opphevelse av et tidligere opphør av ungdomsprogrammet, f.eks. når opphørsdato/sluttdato ble
 * satt feil eller bruker har fått medhold i klage på opphøret. Denne behandles uavhengig av øvrig
 * programperiode-logikk i {@link ProgramPeriodeStrategy}, siden årsaken er en egen, distinkt behandlingsårsak.
 * <p>
 * Brevet sendes kun dersom opphøret faktisk ble vedtatt/iverksatt tidligere (reell <b>opphevelse</b>), se
 * {@link OpphørOpphevetUtleder}. Dersom opphøret aldri ble iverksatt (opphør og opphevelse slått sammen på
 * samme, fortsatt åpne behandling), er dette i stedet en <b>annullering</b> av opphøret, og det sendes ikke
 * noe brev — det finnes ikke noe opphørsvedtak for bruker å oppheve.
 */
@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class OpphørOpphevetStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final OpphørOpphevetInnholdBygger opphørOpphevetInnholdBygger;
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;

    @Inject
    public OpphørOpphevetStrategy(OpphørOpphevetInnholdBygger opphørOpphevetInnholdBygger,
                                   UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.opphørOpphevetInnholdBygger = opphørOpphevetInnholdBygger;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
    }

    @Override
    public List<VedtaksbrevStrategyResultat> evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultater = new ResultatHelper(VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat));
        if (resultater.innholder(DetaljertResultatType.OPPHØR_OPPHEVET)
            && OpphørOpphevetUtleder.opphørVarFaktiskIverksatt(behandling, ungdomsprogramPeriodeRepository)) {
            return List.of(VedtaksbrevStrategyResultat.medUredigerbarBrev(
                DokumentMalType.OPPHOR_OPPHEVET_DOK, opphørOpphevetInnholdBygger,
                "Automatisk brev ved opphevelse av opphør."));
        }
        return List.of();
    }

}

