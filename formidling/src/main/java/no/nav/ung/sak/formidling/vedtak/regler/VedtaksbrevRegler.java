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

import java.util.Set;
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
        Set<VedtaksbrevStrategyResultat> resultat = innholdbyggerStrategies.stream()
            .filter(it -> it.skalEvaluere(behandling, detaljertResultat))
            .map(it -> it.evaluer(behandling, detaljertResultat))
            .collect(Collectors.toSet());

        var redigerRegelResultat = harUtførteManuelleAksjonspunkterMedToTrinn(behandling);

        boolean skalIkkeHaBrev = !resultat.isEmpty() && resultat.stream().anyMatch(it -> it.bygger() == null);
        if (skalIkkeHaBrev) {
            return ingenBrevResultat(detaljertResultat, redigerRegelResultat, resultat);
        }

        boolean harBygger = !resultat.isEmpty() && resultat.stream().allMatch(it -> it.bygger() != null);
        if (harBygger) {
            return automatiskBrevResultat(detaljertResultat, resultat, redigerRegelResultat);
        }

        if (redigerRegelResultat.kanRedigere()) {
            // ingen automatisk brev, men har ap så tilbyr tom brev for redigering
            return tomManueltBrev(detaljertResultat, redigerRegelResultat);
        }

        var resultaterInfo = detaljertResultat
            .toSegments().stream()
            .flatMap(it -> it.getValue().resultatInfo().stream())
            .collect(Collectors.toSet());

        String forklaring = "Ingen brev ved resultater: %s".formatted(String.join(", ", resultaterInfo.stream().map(DetaljertResultatInfo::utledForklaring).toList()));
        return VedtaksbrevRegelResulat.ingenBrev(detaljertResultat, IngenBrevÅrsakType.IKKE_IMPLEMENTERT, forklaring);
    }

    @NotNull
    private VedtaksbrevRegelResulat ingenBrevResultat(LocalDateTimeline<DetaljertResultat> detaljertResultat, RedigerRegelResultat redigerRegelResultat, Set<VedtaksbrevStrategyResultat> resultat) {
        if (redigerRegelResultat.kanRedigere()) {
            // ingen brev, men har ap så tilbyr tom brev for redigering
            return tomManueltBrev(detaljertResultat, redigerRegelResultat);
        }

        var byggerResultat = resultat.stream().findFirst().orElseThrow();
        return VedtaksbrevRegelResulat.ingenBrev(
            detaljertResultat, byggerResultat.ingenBrevÅrsakType(), byggerResultat.forklaring()
        );
    }

    @NotNull
    private VedtaksbrevRegelResulat tomManueltBrev(LocalDateTimeline<DetaljertResultat> detaljertResultat, RedigerRegelResultat redigerRegelResultat) {
        String forklaring = "Tom fritekstbrev pga manuelle aksjonspunkter. " + redigerRegelResultat.forklaring();
        return VedtaksbrevRegelResulat.tomRedigerbarBrev(
            manueltVedtaksbrevInnholdBygger,
            detaljertResultat,
            forklaring
        );
    }

    @NotNull
    private static VedtaksbrevRegelResulat automatiskBrevResultat(LocalDateTimeline<DetaljertResultat> detaljertResultat, Set<VedtaksbrevStrategyResultat> resultat, RedigerRegelResultat redigerRegelResultat) {
        VedtaksbrevStrategyResultat vedtaksbrevStrategyResultat = resultat.stream().findFirst().orElseThrow();
        return VedtaksbrevRegelResulat.automatiskBrev(
            vedtaksbrevStrategyResultat.bygger(),
            detaljertResultat,
            vedtaksbrevStrategyResultat.forklaring() + " " + redigerRegelResultat.forklaring(),
            redigerRegelResultat.kanRedigere()
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
