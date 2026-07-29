package no.nav.ung.sak.formidling.vedtak.regler;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.Presedens;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevInnholdbyggerStrategy;
import no.nav.ung.sak.formidling.vedtak.regler.strategy.VedtaksbrevStrategyResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultatTidslinjeUtleder;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertVilkårResultat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;


@ApplicationScoped
@FagsakYtelseTypeRef
@BehandlingTypeRef(BehandlingType.FØRSTEGANGSSØKNAD)
@BehandlingTypeRef(BehandlingType.REVURDERING)
public class YtelseVedtaksbrevRegler implements VedtaksbrevRegel {

    private static final Logger LOG = LoggerFactory.getLogger(YtelseVedtaksbrevRegler.class);

    private BehandlingRepository behandlingRepository;
    private Instance<DetaljertResultatTidslinjeUtleder> detaljertResultatUtledere;
    private Instance<VedtaksbrevInnholdbyggerStrategy> innholdbyggerStrategiesInstances;

    public YtelseVedtaksbrevRegler() {
    }

    @Inject
    public YtelseVedtaksbrevRegler(
        BehandlingRepository behandlingRepository,
        @Any Instance<DetaljertResultatTidslinjeUtleder> detaljertResultatUtledere,
        @Any Instance<VedtaksbrevInnholdbyggerStrategy> innholdbyggerStrategiesInstances
    ) {
        this.behandlingRepository = behandlingRepository;
        this.detaljertResultatUtledere = detaljertResultatUtledere;
        this.innholdbyggerStrategiesInstances = innholdbyggerStrategiesInstances;
    }

    @Override
    public BehandlingVedtaksbrevResultat kjør(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var detaljertResultatUtleder = FagsakYtelseTypeRef.Lookup.find(detaljertResultatUtledere, behandling.getFagsakYtelseType()).orElseThrow();
        LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje = detaljertResultatUtleder.utledDetaljertResultat(behandling);
        return bestemResultat(behandling, detaljertResultatTidslinje);
    }

    private BehandlingVedtaksbrevResultat bestemResultat(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var innholdbyggerStrategies = innholdbyggerStrategiesInstances.select(new FagsakYtelseTypeRef.FagsakYtelseTypeRefLiteral(behandling.getFagsakYtelseType()));

        var kandidater = innholdbyggerStrategies.stream().toList();

        var overstyrendeIngenBrev = kandidater.stream()
            .filter(it -> it.presedens() == Presedens.OVERSTYRENDE_INGEN_BREV)
            .flatMap(it -> it.evaluer(behandling, detaljertResultat).stream())
            .toList();
        if (!overstyrendeIngenBrev.isEmpty()) {
            return lagIngenBrevResultat(detaljertResultat, overstyrendeIngenBrev);
        }

        var overstyrendeEnkeltbrev = kandidater.stream()
            .filter(it -> it.presedens() == Presedens.OVERSTYRENDE_ENKELTBREV)
            .flatMap(it -> it.evaluer(behandling, detaljertResultat).stream())
            .toList().stream()
            .filter(it -> it.bygger() != null)
            .toList();
        if (overstyrendeEnkeltbrev.size() > 1) {
            throw new IllegalStateException("Flere overstyrende enkeltbrev-strategier ga resultat, forventet maks ett: "
                + overstyrendeEnkeltbrev.stream().map(VedtaksbrevStrategyResultat::forklaring).collect(Collectors.joining(", ")));
        }
        if (!overstyrendeEnkeltbrev.isEmpty()) {
            return lagBrevResultat(detaljertResultat, overstyrendeEnkeltbrev);
        }

        // 3. Normale, additive strategier.
        var normaleResultater = kandidater.stream()
            .filter(it -> it.presedens() == Presedens.NORMAL)
            .flatMap(it -> it.evaluer(behandling, detaljertResultat).stream())
            .toList();

        if (normaleResultater.size() > 1) {
            LOG.info("Flere resultater for strategier: {}", normaleResultater.stream()
                .map(VedtaksbrevStrategyResultat::forklaring)
                .collect(Collectors.joining(", ")));
        }

        var brevResultater = normaleResultater.stream()
            .filter(it -> it.bygger() != null)
            .toList();
        if (!brevResultater.isEmpty()) {
            return lagBrevResultat(detaljertResultat, brevResultater);
        }

        var ingenBrevResultat = normaleResultater.stream()
            .filter(it -> it.bygger() == null)
            .toList();
        if (!ingenBrevResultat.isEmpty()) {
            return lagIngenBrevResultat(detaljertResultat, ingenBrevResultat);
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
        return BehandlingVedtaksbrevResultat.utenBrev(detaljertResultat, List.of(
            VedtaksbrevRegelResultat.ingenBrev(IngenBrevÅrsakType.IKKE_IMPLEMENTERT, utledForklaring(detaljertResultat)
            )));
    }

    /**
     * Ingen strategi kjente seg igjen. Oppsummerer grunnlaget behandlingen ble vurdert på, slik at det er mulig å
     * se hvorfor ingen brev ble utledet.
     */
    private static String utledForklaring(LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var tilVurdering = DetaljertResultat.kunTilVurdering(detaljertResultat);
        if (tilVurdering.isEmpty()) {
            return "Ingen brev - ingen perioder til vurdering.";
        }

        var årsaker = tilVurdering.stream()
            .flatMap(it -> it.getValue().behandlingsårsaker().stream())
            .map(BehandlingÅrsakType::getKode)
            .distinct().sorted().toList();

        var avslåtteVilkår = vilkårTyper(tilVurdering, DetaljertResultat::avslåtteVilkår);
        var ikkeVurderteVilkår = vilkårTyper(tilVurdering, DetaljertResultat::ikkeVurderteVilkår);
        var harUtbetaling = tilVurdering.stream().anyMatch(it -> it.getValue().harPositivUtbetaling());

        return "Ingen brev for perioder %s med behandlingsårsaker %s, avslåtte vilkår %s, ikke vurderte vilkår %s, utbetaling: %s."
            .formatted(tilVurdering.getLocalDateIntervals(), årsaker, avslåtteVilkår, ikkeVurderteVilkår, harUtbetaling ? "ja" : "nei");
    }

    private static List<String> vilkårTyper(LocalDateTimeline<DetaljertResultat> tidslinje, Function<DetaljertResultat, Set<DetaljertVilkårResultat>> velger) {
        return tidslinje.stream()
            .flatMap(it -> velger.apply(it.getValue()).stream())
            .map(it -> it.vilkårType().getKode())
            .distinct().sorted().toList();
    }


}
