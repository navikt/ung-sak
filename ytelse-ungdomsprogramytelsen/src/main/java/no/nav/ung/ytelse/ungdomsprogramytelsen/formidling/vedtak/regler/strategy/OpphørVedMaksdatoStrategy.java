package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler.strategy;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.resultat.ResultatHelper;
import no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold.OpphørVedMaksdatoInnholdBygger;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public final class OpphørVedMaksdatoStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final OpphørVedMaksdatoInnholdBygger opphørVedMaksdatoInnholdBygger;


    @Inject
    public OpphørVedMaksdatoStrategy(OpphørVedMaksdatoInnholdBygger opphørVedMaksdatoInnholdBygger, UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository) {
        this.opphørVedMaksdatoInnholdBygger = opphørVedMaksdatoInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return VedtaksbrevStrategyResultat.medUredigerbarBrev(
            DokumentMalType.OPPHOR_VED_MAKSDATO_DOK, opphørVedMaksdatoInnholdBygger,
            "Automatisk brev ved opphør grunnet maksdato.");
    }


    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        var resultater = new ResultatHelper(resultatInfo);

        return resultater.innholderIkke(DetaljertResultatType.INNVILGELSE_UTBETALING)
            && resultater.innholderIkke(DetaljertResultatType.ENDRING_SLUTTDATO)
            && resultater.innholder(DetaljertResultatType.OPPHØR_VED_MAKSDATO);
    }

}

