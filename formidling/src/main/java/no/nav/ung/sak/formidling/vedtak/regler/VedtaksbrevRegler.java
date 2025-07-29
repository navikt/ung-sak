package no.nav.ung.sak.formidling.vedtak.regler;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.ung.kodeverk.uttak.Tid;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.formidling.innhold.*;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatInfo;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtleder;
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
    private final UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private final UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private final boolean enableAutoBrevVedBarnDødsfall;
    private final Instance<VedtaksbrevInnholdbyggerStrategy> innholdByggerVelger;

    @Inject
    public VedtaksbrevRegler(
        BehandlingRepository behandlingRepository,
        @Any Instance<VedtaksbrevInnholdBygger> innholdByggere,
        DetaljertResultatUtleder detaljertResultatUtleder,
        UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository,
        UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository,
        @KonfigVerdi(value = "ENABLE_AUTO_BREV_BARN_DØDSFALL", defaultVerdi = "false") boolean enableAutoBrevVedBarnDødsfall,
        @Any Instance<VedtaksbrevInnholdbyggerStrategy> innholdByggerVelger) {
        this.behandlingRepository = behandlingRepository;
        this.innholdByggere = innholdByggere;
        this.detaljertResultatUtleder = detaljertResultatUtleder;
        this.ungdomsprogramPeriodeRepository = ungdomsprogramPeriodeRepository;
        this.ungdomsytelseGrunnlagRepository = ungdomsytelseGrunnlagRepository;
        this.enableAutoBrevVedBarnDødsfall = enableAutoBrevVedBarnDødsfall;
        this.innholdByggerVelger = innholdByggerVelger;
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


        Set<ByggerResultat> resultat = innholdByggerVelger.stream()
            .filter(it -> it.skalEvaluere(behandling, detaljertResultat))
            .map(it -> it.evaluer(behandling, detaljertResultat))
            .collect(Collectors.toSet());

        var redigerRegelResultat = harUtførteManuelleAksjonspunkterMedToTrinn(behandling);
        if (!resultat.isEmpty()) {
            ByggerResultat byggerResultat = resultat.stream().findFirst().orElseThrow();
            return VedtaksbrevRegelResulat.automatiskBrev(
                byggerResultat.bygger(),
                detaljertResultat,
                byggerResultat.forklaring() + " " + redigerRegelResultat.forklaring(),
                redigerRegelResultat.kanRedigere()
            );
        }

        var resultaterInfo = detaljertResultat
            .toSegments().stream()
            .flatMap(it -> it.getValue().resultatInfo().stream())
            .collect(Collectors.toSet());

        var resultater = new ResultatHelper(resultaterInfo);

        if (resultater
            .utenom(DetaljertResultatType.INNVILGET_UTEN_ÅRSAK)
            .innholderBare(DetaljertResultatType.ENDRING_STARTDATO)) {
            String forklaring = "Automatisk brev ved endring av startdato. " + redigerRegelResultat.forklaring();
            return VedtaksbrevRegelResulat.automatiskBrev(
                innholdByggere.select(EndringProgramPeriodeInnholdBygger.class).get(),
                detaljertResultat,
                forklaring,
                redigerRegelResultat.kanRedigere()
            );
        }


        if (resultater.innholder(DetaljertResultatType.KONTROLLER_INNTEKT_REDUKSJON)) {
            String forklaring = "Automatisk brev ved endring av rapportert inntekt. " + redigerRegelResultat.forklaring();
            return VedtaksbrevRegelResulat.automatiskBrev(
                innholdByggere.select(EndringRapportertInntektInnholdBygger.class).get(),
                detaljertResultat,
                forklaring,
                redigerRegelResultat.kanRedigere()
            );
        }

        if (resultater.innholderBare(DetaljertResultatType.ENDRING_ØKT_SATS)) {
            String forklaring = "Automatisk brev ved endring til høy sats. " + redigerRegelResultat.forklaring();
            return VedtaksbrevRegelResulat.automatiskBrev(
                innholdByggere.select(EndringHøySatsInnholdBygger.class).get(),
                detaljertResultat,
                forklaring,
                redigerRegelResultat.kanRedigere()
            );
        }

        if (resultater.innholderBare(DetaljertResultatType.ENDRING_BARN_FØDSEL)) {
            String forklaring = "Automatisk brev ved fødsel av barn. " + redigerRegelResultat.forklaring();
            return VedtaksbrevRegelResulat.automatiskBrev(
                innholdByggere.select(EndringBarnetilleggInnholdBygger.class).get(),
                detaljertResultat,
                forklaring,
                redigerRegelResultat.kanRedigere()
            );
        }

        if (redigerRegelResultat.kanRedigere()) {
            // ingen automatisk brev, men har ap så tilbyr tom brev for redigering
            String forklaring = "Tom fritekstbrev pga manuelle aksjonspunkter. " + redigerRegelResultat.forklaring();
            return VedtaksbrevRegelResulat.tomRedigerbarBrev(
                innholdByggere.select(ManueltVedtaksbrevInnholdBygger.class).get(),
                detaljertResultat,
                forklaring
            );
        }

        if (resultater.innholderBare(DetaljertResultatType.KONTROLLER_INNTEKT_FULL_UTBETALING)) {
            String forklaring = "Ingen brev ved full utbetaling etter kontroll av inntekt.";
            return VedtaksbrevRegelResulat.ingenBrev(
                detaljertResultat,
                IngenBrevÅrsakType.IKKE_RELEVANT,
                forklaring
            );
        }

        String forklaring = "Ingen brev ved resultater: %s".formatted(String.join(", ", resultaterInfo.stream().map(DetaljertResultatInfo::utledForklaring).toList()));
        return VedtaksbrevRegelResulat.ingenBrev(detaljertResultat, IngenBrevÅrsakType.IKKE_IMPLEMENTERT, forklaring);
    }

    private boolean erFørsteOpphør(Behandling behandling) {
        var forrigeGrunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getOriginalBehandlingId().orElseThrow(
            () -> new IllegalStateException("Må ha original behandling ved opphør")
        )).orElseThrow(() -> new IllegalStateException("Mangler grunnlag for forrige behandling"));
        return forrigeGrunnlag.getUngdomsprogramPerioder().getPerioder().stream()
            .anyMatch(it -> Tid.TIDENES_ENDE.equals(it.getPeriode().getTomDato()));
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
