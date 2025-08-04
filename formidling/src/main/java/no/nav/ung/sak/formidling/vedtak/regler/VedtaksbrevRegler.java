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

    public BehandlingVedtaksbrevResultat kjør(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje = detaljertResultatUtleder.utledDetaljertResultat(behandling);
        return bestemResultat(behandling, detaljertResultatTidslinje);
    }

    private BehandlingVedtaksbrevResultat bestemResultat(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var strategyResultater = innholdbyggerStrategies.stream()
            .filter(it -> it.skalEvaluere(behandling, detaljertResultat))
            .map(it -> it.evaluer(behandling, detaljertResultat))
            .toList();


        if (strategyResultater.size() > 1) {
            LOG.info("Flere resultater for strategier: {}", strategyResultater.stream()
                .map(VedtaksbrevStrategyResultat::forklaring)
                .collect(Collectors.joining(", ")));
        }

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
            var automatiskVedtaksbrevResultater = byggAutomatiskVedtaksbrevResultat(automatiskBrevResultat, redigerRegelResultat);
            return new BehandlingVedtaksbrevResultat(true, detaljertResultat, automatiskVedtaksbrevResultater);
        }

        if (redigerRegelResultat.kanRedigere()) {
            // ingen automatisk brev, men har ap så tilbyr tom brev for redigering
            return new BehandlingVedtaksbrevResultat(true, detaljertResultat,
                List.of(VedtaksbrevResultat.tomRedigerbarBrev(
                    manueltVedtaksbrevInnholdBygger,
                    "Tom fritekstbrev pga manuelle aksjonspunkter: " + redigerRegelResultat.forklaring()))
            );
        }

        var resultaterInfo = detaljertResultat
            .toSegments().stream()
            .flatMap(it -> it.getValue().resultatInfo().stream())
            .collect(Collectors.toSet());

        String forklaring = "Ingen brev ved resultater: %s".formatted(String.join(", ", resultaterInfo.stream().map(DetaljertResultatInfo::utledForklaring).toList()));
        return new BehandlingVedtaksbrevResultat(false, detaljertResultat, List.of(VedtaksbrevResultat.ingenBrev(IngenBrevÅrsakType.IKKE_IMPLEMENTERT, forklaring)));
    }

    private BehandlingVedtaksbrevResultat håndterIngenBrevResultat(
        LocalDateTimeline<DetaljertResultat> detaljertResultat,
        RedigerRegelResultat redigerRegelResultat,
        List<VedtaksbrevStrategyResultat> ingenBrevResultat) {

        if (redigerRegelResultat.kanRedigere()) {
            String forklaringer = ingenBrevResultat.stream().map(VedtaksbrevStrategyResultat::forklaring).collect(Collectors.joining(", "));

            // ingen brev, men har ap så tilbyr tom brev for redigering
            String forklaring = "Tom fritekstbrev pga manuelle aksjonspunkter: %s. Ingen automatisk brev pga: %s."
                .formatted(redigerRegelResultat.forklaring(), forklaringer);
            return new BehandlingVedtaksbrevResultat(
                true,
                detaljertResultat,
                List.of(VedtaksbrevResultat.tomRedigerbarBrev(
                    manueltVedtaksbrevInnholdBygger,
                    forklaring
                )));
        }

        return new BehandlingVedtaksbrevResultat(
            false,
            detaljertResultat,
            ingenBrevResultat.stream().map(it -> VedtaksbrevResultat.ingenBrev(it.ingenBrevÅrsakType(), it.forklaring())).toList()
        );

    }


    private static List<VedtaksbrevResultat> byggAutomatiskVedtaksbrevResultat(
        List<VedtaksbrevStrategyResultat> resultat,
        RedigerRegelResultat redigerRegelResultat) {

        return resultat.stream()
            .map(it -> {
                String forklaring = it.forklaring() + "." + (redigerRegelResultat.kanRedigere() ? " Kan redigeres pga " + redigerRegelResultat.forklaring() : "");
                    return VedtaksbrevResultat
                        .automatiskBrev(
                            it.dokumentMalType(),
                            it.bygger(),
                            forklaring,
                            redigerRegelResultat.kanRedigere());
                }
            ).toList();
    }

    private static RedigerRegelResultat harUtførteManuelleAksjonspunkterMedToTrinn(Behandling behandling) {
        var lukkedeApMedToTrinn = behandling.getAksjonspunkterMedTotrinnskontroll().stream()
            .filter(Aksjonspunkt::erUtført).toList();
        boolean kanRedigere = !lukkedeApMedToTrinn.isEmpty();
        if (kanRedigere) {
            return new RedigerRegelResultat(true, "aksjonspunkt(er) %s".formatted(
                lukkedeApMedToTrinn.stream().map(it -> it.getAksjonspunktDefinisjon().getKode())
                    .collect(Collectors.toSet())
            ));
        }
        return new RedigerRegelResultat(false, "");
    }


    private record RedigerRegelResultat(boolean kanRedigere, String forklaring) {
    }

}
