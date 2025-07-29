package no.nav.ung.sak.formidling.vedtak.regler;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.formidling.innhold.ManueltVedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
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
    private final Instance<VedtaksbrevInnholdBygger> innholdByggere;
    private final DetaljertResultatUtleder detaljertResultatUtleder;
    private final UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private final boolean enableAutoBrevVedBarnDødsfall;
    private final Instance<VedtaksbrevInnholdbyggerStrategy> innholdbyggerStrategies;
    private final ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger;

    @Inject
    public VedtaksbrevRegler(
        BehandlingRepository behandlingRepository,
        @Any Instance<VedtaksbrevInnholdBygger> innholdByggere,
        DetaljertResultatUtleder detaljertResultatUtleder,
        UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
        @KonfigVerdi(value = "ENABLE_AUTO_BREV_BARN_DØDSFALL", defaultVerdi = "false") boolean enableAutoBrevVedBarnDødsfall,
        @Any Instance<VedtaksbrevInnholdbyggerStrategy> innholdbyggerStrategies,
        ManueltVedtaksbrevInnholdBygger manueltVedtaksbrevInnholdBygger) {
        this.behandlingRepository = behandlingRepository;
        this.innholdByggere = innholdByggere;
        this.detaljertResultatUtleder = detaljertResultatUtleder;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.enableAutoBrevVedBarnDødsfall = enableAutoBrevVedBarnDødsfall;
        this.innholdbyggerStrategies = innholdbyggerStrategies;
        this.manueltVedtaksbrevInnholdBygger = manueltVedtaksbrevInnholdBygger;
    }

    public VedtaksbrevRegelResulat kjør(Long behandlingId) {
        //TODO flytt dette til DetaljertResultatUtleder, bruk combinator til å finne resultat. Refactor førstegangsbygger til å bruke dette istedenfor å utlede selv.
        // .... Du har laget test for førstegangsbehandling, så få den til å kjøre
        var ungdomsytelseGrunnlag = ungdomsytelseGrunnlagRepository.hentGrunnlag(behandlingId);
        if (!enableAutoBrevVedBarnDødsfall && ungdomsytelseGrunnlag.isPresent()) {
            LocalDateTimeline<UngdomsytelseSatser> satsTidslinje = ungdomsytelseGrunnlag.get().getSatsTidslinje();
            var satsSegments = satsTidslinje.toSegments();
            LocalDateSegment<UngdomsytelseSatser> previous = null;
            for (LocalDateSegment<UngdomsytelseSatser> current : satsSegments) {
                if (previous == null) {
                    previous = current;
                    continue;
                }
                if (SatsEndring.bestemSatsendring(current.getValue(), previous.getValue()).dødsfallBarn()) {
                    return VedtaksbrevRegelResulat.ingenBrev(
                        LocalDateTimeline.empty(),
                        IngenBrevÅrsakType.IKKE_IMPLEMENTERT,
                        "Ingen brev ved dødsfall av barn."
                    );
                }
                previous = current;
            }
        }

        var behandling = behandlingRepository.hentBehandling(behandlingId);
        LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje = detaljertResultatUtleder.utledDetaljertResultat(behandling);
        return bestemResultat(behandling, detaljertResultatTidslinje);
    }

    private VedtaksbrevRegelResulat bestemResultat(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        Set<ByggerResultat> resultat = innholdbyggerStrategies.stream()
            .filter(it -> it.skalEvaluere(behandling, detaljertResultat))
            .map(it -> it.evaluer(behandling, detaljertResultat))
            .collect(Collectors.toSet());

        var redigerRegelResultat = harUtførteManuelleAksjonspunkterMedToTrinn(behandling);
        boolean harBygger = !resultat.isEmpty() && resultat.stream().allMatch(it -> it.bygger() != null);
        if (harBygger) {
            return automatiskBrevResultat(detaljertResultat, resultat, redigerRegelResultat);
        }

        if (redigerRegelResultat.kanRedigere()) {
            // ingen automatisk brev, men har ap så tilbyr tom brev for redigering
            String forklaring = "Tom fritekstbrev pga manuelle aksjonspunkter. " + redigerRegelResultat.forklaring();
            return VedtaksbrevRegelResulat.tomRedigerbarBrev(
                manueltVedtaksbrevInnholdBygger,
                detaljertResultat,
                forklaring
            );
        }

        boolean skalIkkeHaBrev = !resultat.isEmpty() && resultat.stream().allMatch(it -> it.bygger() == null);
        if (skalIkkeHaBrev) {
            ByggerResultat byggerResultat = resultat.stream().findFirst().orElseThrow();
            return VedtaksbrevRegelResulat.ingenBrev(
                detaljertResultat, byggerResultat.ingenBrevÅrsakType(), byggerResultat.forklaring()
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
    private static VedtaksbrevRegelResulat automatiskBrevResultat(LocalDateTimeline<DetaljertResultat> detaljertResultat, Set<ByggerResultat> resultat, RedigerRegelResultat redigerRegelResultat) {
        ByggerResultat byggerResultat = resultat.stream().findFirst().orElseThrow();
        return VedtaksbrevRegelResulat.automatiskBrev(
            byggerResultat.bygger(),
            detaljertResultat,
            byggerResultat.forklaring() + " " + redigerRegelResultat.forklaring(),
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
