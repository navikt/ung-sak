package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.vedtak.regler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.vedtak.regler.*;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatInfo;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
@BehandlingTypeRef(BehandlingType.REVURDERING)
public class UngVedtaksbrevRegler implements VedtaksbrevRegel {

    private static final Logger LOG = LoggerFactory.getLogger(UngVedtaksbrevRegler.class);

    private BehandlingRepository behandlingRepository;
    private DetaljertResultatUtleder detaljertResultatUtleder;
    private Instance<VedtaksbrevInnholdbyggerStrategy> innholdbyggerStrategies;

    public UngVedtaksbrevRegler() {
    }

    @Inject
    public UngVedtaksbrevRegler(
        BehandlingRepository behandlingRepository,
        DetaljertResultatUtleder detaljertResultatUtleder,
        @Any Instance<VedtaksbrevInnholdbyggerStrategy> innholdbyggerStrategies
    ) {
        this.behandlingRepository = behandlingRepository;
        this.detaljertResultatUtleder = detaljertResultatUtleder;
        this.innholdbyggerStrategies = innholdbyggerStrategies;
    }

    @Override
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

        var ingenBrevResultat = strategyResultater.stream()
            .filter(it -> it.bygger() == null)
            .toList();

        if (!ingenBrevResultat.isEmpty()) {
            return lagIngenBrevResultat(detaljertResultat, ingenBrevResultat);
        }

        var brevResultater = strategyResultater.stream()
            .filter(it -> it.bygger() != null)
            .toList();

        if (!brevResultater.isEmpty()) {
            return lagBrevResultat(detaljertResultat, brevResultater);
        }

        //Fallback for ukjente brev
        return lagIkkeImplementertBrevResultat(detaljertResultat);
    }

    private static BehandlingVedtaksbrevResultat lagIngenBrevResultat(LocalDateTimeline<DetaljertResultat> detaljertResultat, List<VedtaksbrevStrategyResultat> ingenBrevResultat) {
        return BehandlingVedtaksbrevResultat.utenBrev(detaljertResultat,
            ingenBrevResultat.stream()
                .map(it -> VedtaksbrevRegelResultat.ingenBrev(
                    it.ingenBrevÅrsakType(), it.forklaring()))
                .toList()
        );
    }

    private static BehandlingVedtaksbrevResultat lagBrevResultat(LocalDateTimeline<DetaljertResultat> detaljertResultat, List<VedtaksbrevStrategyResultat> brevResultater) {
        var vedtaksbrev = brevResultater.stream()
            .map(it -> new Vedtaksbrev(
                it.dokumentMalType(),
                it.bygger(),
                it.vedtaksbrevEgenskaper(),
                it.forklaring())
            ).toList();
        return BehandlingVedtaksbrevResultat.medBrev(detaljertResultat, vedtaksbrev);
    }

    private static BehandlingVedtaksbrevResultat lagIkkeImplementertBrevResultat(LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultaterInfo = detaljertResultat
            .toSegments().stream()
            .flatMap(it -> it.getValue().resultatInfo().stream())
            .collect(Collectors.toSet());

        String forklaring = "Ingen brev ved resultater: %s".formatted(String.join(", ", resultaterInfo.stream().map(DetaljertResultatInfo::utledForklaring).toList()));
        return BehandlingVedtaksbrevResultat.utenBrev(detaljertResultat, List.of(
            VedtaksbrevRegelResultat.ingenBrev(IngenBrevÅrsakType.IKKE_IMPLEMENTERT, forklaring
            )));
    }


}
