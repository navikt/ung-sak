package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.AutomatiskOpphørInnholdBygger;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class AutomatiskOpphørStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final AutomatiskOpphørInnholdBygger automatiskOpphørInnholdBygger;

    @Inject
    public AutomatiskOpphørStrategy(AutomatiskOpphørInnholdBygger automatiskOpphørInnholdBygger) {
        this.automatiskOpphørInnholdBygger = automatiskOpphørInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return VedtaksbrevStrategyResultat.medUredigerbarBrev(
            DokumentMalType.AUTOMATISK_OPPHOR_DOK, automatiskOpphørInnholdBygger,
            "Automatisk brev ved opphør grunnet maksdato.");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        boolean harVarselAutomatiskOpphør = behandling.getBehandlingÅrsakerTyper()
            .contains(BehandlingÅrsakType.RE_VARSEL_AUTOMATISK_OPPHOR);
        if (!harVarselAutomatiskOpphør) {
            return false;
        }
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);
        return resultater.innholder(DetaljertResultatType.ENDRING_SLUTTDATO);
    }
}

