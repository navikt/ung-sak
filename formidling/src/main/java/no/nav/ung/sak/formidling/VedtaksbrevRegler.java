package no.nav.ung.sak.formidling;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.innhold.*;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatInfo;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Dependent
public class VedtaksbrevRegler {

    private final BehandlingRepository behandlingRepository;
    private final Instance<VedtaksbrevInnholdBygger> innholdByggere;
    private final DetaljertResultatUtleder detaljertResultatUtleder;
    private static final Logger LOG = LoggerFactory.getLogger(VedtaksbrevRegler.class);

    @Inject
    public VedtaksbrevRegler(
        BehandlingRepository behandlingRepository,
        @Any Instance<VedtaksbrevInnholdBygger> innholdByggere,
        DetaljertResultatUtleder detaljertResultatUtleder) {
        this.behandlingRepository = behandlingRepository;
        this.innholdByggere = innholdByggere;
        this.detaljertResultatUtleder = detaljertResultatUtleder;
    }

    public VedtaksbrevRegelResulat kjør(Long id) {
        var behandling = behandlingRepository.hentBehandling(id);
        LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje = detaljertResultatUtleder.utledDetaljertResultat(behandling);
        return lagResultatMedBygger(behandling, detaljertResultatTidslinje);
    }

    private VedtaksbrevRegelResulat lagResultatMedBygger(Behandling behandling, LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultaterInfo = detaljertResultat
            .toSegments().stream()
            .flatMap(it -> it.getValue().resultatInfo().stream())
            .collect(Collectors.toSet());

        var resultater = new ResultatHelper(resultaterInfo);

        var redigerRegelResultat = harUtførteManuelleAksjonspunkterMedToTrinn(behandling);

        if (resultater
            .utenom(DetaljertResultatType.INNVILGELSE_VILKÅR_NY_PERIODE)
            .innholderBare(DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE)) {
            String forklaring = "Automatisk brev ved ny innvilgelse. " + redigerRegelResultat.forklaring();
            return VedtaksbrevRegelResulat.automatiskBrev(
                innholdByggere.select(FørstegangsInnvilgelseInnholdBygger.class).get(),
                detaljertResultat,
                forklaring,
                redigerRegelResultat.kanRedigere()
            );
        }

        if (resultater
            .utenom(DetaljertResultatType.UENDRET_INNVILGET)
            .innholderBare(DetaljertResultatType.OPPHØR)) {
            String forklaring = "Automatisk brev ved opphør. " + redigerRegelResultat.forklaring();
            return VedtaksbrevRegelResulat.automatiskBrev(
                innholdByggere.select(OpphørInnholdBygger.class).get(),
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
                innholdByggere.select(ManuellVedtaksbrevInnholdBygger.class).get(),
                detaljertResultat,
                forklaring
            );
        }

        String forklaring = "Ingen brev ved resultater: %s".formatted(String.join(", ", resultaterInfo.stream().map(DetaljertResultatInfo::utledForklaring).toList()));
        return VedtaksbrevRegelResulat.ingenBrev(detaljertResultat, forklaring);
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


    private record RedigerRegelResultat(boolean kanRedigere, String forklaring) {}

    private static class ResultatHelper {
        private final Set<DetaljertResultatInfo> resultatInfo;
        private final Set<DetaljertResultatType> resultatTyper;

        ResultatHelper(Set<DetaljertResultatInfo> resultatInfo) {
            this.resultatInfo = resultatInfo;
            this.resultatTyper = resultatInfo.stream()
                .map(DetaljertResultatInfo::detaljertResultatType)
                .collect(Collectors.toSet());
        }

        boolean innholderBare(DetaljertResultatType... typer) {
            return resultatTyper.equals(Arrays.stream(typer).collect(Collectors.toSet()));
        }

        ResultatHelper utenom(DetaljertResultatType... typer) {
            var typerSet = Arrays.stream(typer).collect(Collectors.toSet());
            var filtrert = resultatInfo.stream()
                .filter(it -> !typerSet.contains(it.detaljertResultatType()))
                .collect(Collectors.toSet());
            return new ResultatHelper(filtrert);
        }

        public boolean innholder(DetaljertResultatType detaljertResultatType) {
            return resultatTyper.contains(detaljertResultatType);
        }
    }
}
