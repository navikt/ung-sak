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

        var resultater = resultaterInfo.stream().map(DetaljertResultatInfo::detaljertResultatType).collect(Collectors.toSet());

        var redigerRegelResultat = harUtførteManuelleAksjonspunkterMedToTrinn(behandling);

        if (innholderBare(resultater, DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE)
            || innholderBare(resultater, DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE, DetaljertResultatType.INNVILGELSE_VILKÅR_NY_PERIODE) ) {
            String forklaring = "Automatisk brev ved ny innvilgelse. " + redigerRegelResultat.forklaring();
            return VedtaksbrevRegelResulat.automatiskBrev(
                innholdByggere.select(InnvilgelseInnholdBygger.class).get(),
                detaljertResultat,
                forklaring,
                redigerRegelResultat.kanRedigere()
            );
        }

        if (innholderBare(resultater, DetaljertResultatType.OPPHØR)) {
            String forklaring = "Automatisk brev ved opphør. " + redigerRegelResultat.forklaring();
            return VedtaksbrevRegelResulat.automatiskBrev(
                innholdByggere.select(OpphørInnholdBygger.class).get(),
                detaljertResultat,
                forklaring,
                redigerRegelResultat.kanRedigere()
            );
        }

        if (resultater.contains(DetaljertResultatType.KONTROLLER_INNTEKT_REDUKSJON)) {
            String forklaring = "Automatisk brev ved endring av rapportert inntekt. " + redigerRegelResultat.forklaring();
            return VedtaksbrevRegelResulat.automatiskBrev(
                innholdByggere.select(EndringRapportertInntektInnholdBygger.class).get(),
                detaljertResultat,
                forklaring,
                redigerRegelResultat.kanRedigere()
            );
        }

        if (innholderBare(resultater, DetaljertResultatType.ENDRING_ØKT_SATS)) {
            String forklaring = "Automatisk brev ved endring til høy sats. " + redigerRegelResultat.forklaring();
            return VedtaksbrevRegelResulat.automatiskBrev(
                innholdByggere.select(EndringHøySatsInnholdBygger.class).get(),
                detaljertResultat,
                forklaring,
                redigerRegelResultat.kanRedigere()
            );
        }

        if (innholderBare(resultater, DetaljertResultatType.ENDRING_BARN_FØDSEL)) {
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


    @SafeVarargs
    private static <V> boolean innholderBare(Set<V> set, V... value) {
        return set.equals(Arrays.stream(value).collect(Collectors.toSet()));
    }

    private record RedigerRegelResultat(boolean kanRedigere, String forklaring) {}
}
