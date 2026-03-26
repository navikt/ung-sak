package no.nav.ung.ytelse.aktivitetspenger.formidling.vedtak;

import jakarta.enterprise.context.Dependent;
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
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatInfo;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatType;
import no.nav.ung.ytelse.aktivitetspenger.formidling.innhold.FørstegangsAvslagInnholdBygger;

import java.util.stream.Collectors;

@Dependent
@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
public final class AvslagInngangsvilkårStrategy implements VedtaksbrevInnholdbyggerStrategy {

    private final FørstegangsAvslagInnholdBygger førstegangsAvslagInnholdBygger;

    @Inject
    public AvslagInngangsvilkårStrategy(FørstegangsAvslagInnholdBygger førstegangsAvslagInnholdBygger) {
        this.førstegangsAvslagInnholdBygger = førstegangsAvslagInnholdBygger;
    }

    @Override
    public VedtaksbrevStrategyResultat evaluer(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        return new VedtaksbrevStrategyResultat(
            DokumentMalType.AVSLAG__DOK,
            førstegangsAvslagInnholdBygger,
            new VedtaksbrevEgenskaper(true,
                true,
                true,
                true),
            null,
            "Avslagsbrev ved avslag på inngangsvilkår"
        );
    }

    @Override
    public boolean skalEvaluere(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultatInfo = VedtaksbrevInnholdbyggerStrategy.tilResultatInfo(detaljertResultat);
        return resultatInfo.stream()
            .map(DetaljertResultatInfo::detaljertResultatType)
            .collect(Collectors.toSet()).stream()
            .allMatch(it -> it == DetaljertResultatType.AVSLAG_INNGANGSVILKÅR);
    }
}
