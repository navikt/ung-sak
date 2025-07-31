package no.nav.ung.sak.formidling.vedtak.regler;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatInfo;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtleder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Dependent
public class VedtaksbrevRegler {

    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevRegler.class);

    private final BehandlingRepository behandlingRepository;
    private final DetaljertResultatUtleder detaljertResultatUtleder;
    private final Instance<VedtaksbrevInnholdbyggerStrategy> innholdbyggerStrategies;
    private final ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger;

    @Inject
    public VedtaksbrevRegler(
        BehandlingRepository behandlingRepository,
        DetaljertResultatUtleder detaljertResultatUtleder,
        @Any Instance<VedtaksbrevInnholdbyggerStrategy> innholdbyggerStrategies,
        ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger) {
        this.behandlingRepository = behandlingRepository;
        this.detaljertResultatUtleder = detaljertResultatUtleder;
        this.innholdbyggerStrategies = innholdbyggerStrategies;
        this.manueltVedtaksbrevInnholdBygger = manueltVedtaksbrevInnholdBygger;
    }

    public VedtaksbrevRegelResulat kjør(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje = detaljertResultatUtleder.utledDetaljertResultat(behandling);
        return bestemResultat(behandling, detaljertResultatTidslinje);
    }

    private VedtaksbrevRegelResulat bestemResultat(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var strategyResultater = innholdbyggerStrategies.stream()
            .filter(it -> it.skalEvaluere(behandling, detaljertResultat))
            .map(it -> it.evaluer(behandling, detaljertResultat))
            .toList();

        var redigerRegelResultat = harUtførteManuelleAksjonspunkterMedToTrinn(behandling);

        var ingenBrevResultat = strategyResultater.stream()
            .filter(it -> it.bygger() == null)
            .toList();

        if (!ingenBrevResultat.isEmpty()) {
            return håndterIngenBrevResultat(detaljertResultat, redigerRegelResultat, ingenBrevResultat);
        }

        var automatiskBrevResultat = strategyResultater.stream()
            .filter(it -> it.bygger() != null)
            .toList();
        if (!automatiskBrevResultat.isEmpty()) {
            return håndterAutomatiskBrevResultat(detaljertResultat, automatiskBrevResultat, redigerRegelResultat);
        }

        if (redigerRegelResultat.kanRedigere()) {
            // ingen automatisk brev, men har ap så tilbyr tom brev for redigering
            return VedtaksbrevRegelResulat.tomRedigerbarBrev(
                manueltVedtaksbrevInnholdBygger,
                detaljertResultat,
                "Tom fritekstbrev pga manuelle aksjonspunkter. " + redigerRegelResultat.forklaring()
            );
        }

        var resultaterInfo = detaljertResultat
            .toSegments().stream()
            .flatMap(it -> it.getValue().resultatInfo().stream())
            .collect(Collectors.toSet());

        String forklaring = "Ingen brev ved resultater: %s".formatted(String.join(", ", resultaterInfo.stream().map(DetaljertResultatInfo::utledForklaring).toList()));
        return VedtaksbrevRegelResulat.ingenBrev(detaljertResultat, IngenBrevÅrsakType.IKKE_IMPLEMENTERT, forklaring);
    }

    @NotNull
    private VedtaksbrevRegelResulat håndterIngenBrevResultat(
        LocalDateTimeline<DetaljertResultat> detaljertResultat,
        RedigerRegelResultat redigerRegelResultat,
        List<VedtaksbrevStrategyResultat> ingenBrevResultater) {

        var byggerResultat = ingenBrevResultater.stream().findFirst().orElseThrow(); //TODO håndtere flere resultater
        if (redigerRegelResultat.kanRedigere()) {
            // ingen brev, men har ap så tilbyr tom brev for redigering
            String forklaring = "Tom fritekstbrev pga manuelle aksjonspunkter. %s. Ingen brev pga %s."
                .formatted(redigerRegelResultat.forklaring(), byggerResultat.forklaring());
            return VedtaksbrevRegelResulat.tomRedigerbarBrev(
                manueltVedtaksbrevInnholdBygger,
                detaljertResultat,
                forklaring
            );
        }

        return VedtaksbrevRegelResulat.ingenBrev(
            detaljertResultat, byggerResultat.ingenBrevÅrsakType(), byggerResultat.forklaring()
        );
    }

    private static VedtaksbrevRegelResulat håndterAutomatiskBrevResultat(
        LocalDateTimeline<DetaljertResultat> detaljertResultat,
        List<VedtaksbrevStrategyResultat> resultat,
        RedigerRegelResultat automatiskBrevResultat) {

        var vedtaksbrevStrategyResultat = resultat.stream().findFirst().orElseThrow(); //TODO håndtere flere resultater
        return VedtaksbrevRegelResulat.automatiskBrev(
            vedtaksbrevStrategyResultat.bygger(),
            detaljertResultat,
            vedtaksbrevStrategyResultat.forklaring() + " " + automatiskBrevResultat.forklaring(),
            automatiskBrevResultat.kanRedigere()
        );
    }

    private static RedigerRegelResultat harUtførteManuelleAksjonspunkterMedToTrinn(Behandling behandling) {
        var lukkedeApMedToTrinn = behandling.getAksjonspunkterMedTotrinnskontroll().stream()
            .filter(Aksjonspunkt::erUtført).toList();
        boolean kanRedigere = !lukkedeApMedToTrinn.isEmpty();
        if (kanRedigere) {
            return new RedigerRegelResultat(true, "Kan redigeres pga aksjonspunkt(er) %s".formatted(
                lukkedeApMedToTrinn.stream().map(it -> it.getAksjonspunktDefinisjon().getKode())
                    .collect(Collectors.toSet())
            ));
        }
        return new RedigerRegelResultat(false, "");
    }


    private record RedigerRegelResultat(boolean kanRedigere, String forklaring) {
    }

}
