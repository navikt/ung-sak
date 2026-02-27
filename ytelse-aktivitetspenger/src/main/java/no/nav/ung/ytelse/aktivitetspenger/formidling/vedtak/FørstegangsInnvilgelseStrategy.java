package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatInfo;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.FørstegangsInnvilgelseInnholdBygger;

import java.util.stream.Collectors;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class FørstegangsInnvilgelseStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final FørstegangsInnvilgelseInnholdBygger førstegangsInnvilgelseInnholdBygger;

    @Inject
    public FørstegangsInnvilgelseStrategy(FørstegangsInnvilgelseInnholdBygger førstegangsInnvilgelseInnholdBygger) {
        this.førstegangsInnvilgelseInnholdBygger = førstegangsInnvilgelseInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return VedtaksbrevStrategyResultat.medUredigerbarBrev(DokumentMalType.INNVILGELSE_DOK, førstegangsInnvilgelseInnholdBygger, "Automatisk brev ved ny innvilgelse. ");
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        return resultatInfo.stream()
            .map(DetaljertResultatInfo::detaljertResultatType)
            .collect(Collectors.toSet()).contains(DetaljertResultatType.INNVILGELSE_KUN_VILKÅR); //TODO må endres senere
    }
}
