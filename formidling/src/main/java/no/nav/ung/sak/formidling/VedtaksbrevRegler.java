package no.nav.ung.sak.formidling;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.formidling.innhold.EndringHøySatsInnholdBygger;
import no.nav.ung.sak.formidling.innhold.EndringRapportertInntektInnholdBygger;
import no.nav.ung.sak.formidling.innhold.InnvilgelseInnholdBygger;
import no.nav.ung.sak.formidling.innhold.VedtaksbrevInnholdBygger;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtleder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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
        if (!behandling.erAvsluttet()) {
            LOG.info("Kan ikke bestille brev før behandlingen er avsluttet");
            return null;
        }

        LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje = detaljertResultatUtleder.utledDetaljertResultat(behandling);
        var bygger = bestemBygger(detaljertResultatTidslinje);
        if (bygger == null) {
            LOG.warn("Støtter ikke vedtaksbrev for resultater = {} ", DetaljertResultat.timelineTostring(detaljertResultatTidslinje));
            return null;
        }

        return new VedtaksbrevRegelResulat(bygger, detaljertResultatTidslinje);
    }

    private VedtaksbrevInnholdBygger bestemBygger(LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultater = detaljertResultat
            .toSegments().stream()
            .flatMap(it -> it.getValue().resultatTyper().stream())
            .collect(Collectors.toSet());

        if (innholderBare(resultater, DetaljertResultatType.INNVILGET_NY_PERIODE)) {
            return innholdByggere.select(InnvilgelseInnholdBygger.class).get();
        } else if (innholderBare(resultater, DetaljertResultatType.ENDRING_RAPPORTERT_INNTEKT) || innholderBare(resultater, DetaljertResultatType.AVSLAG_RAPPORTERT_INNTEKT)) {
            return innholdByggere.select(EndringRapportertInntektInnholdBygger.class).get();
        } else if (innholderBare(resultater, DetaljertResultatType.ENDRING_ØKT_SATS)) {
            return innholdByggere.select(EndringHøySatsInnholdBygger.class).get();
        } else {
            return null;
        }

    }

    private static String detaljertResultatString(LocalDateTimeline<DetaljertResultat> detaljertResultatTidslinje) {
        return String.join(", ", detaljertResultatTidslinje.toSegments().stream()
            .map(it -> it.getLocalDateInterval().toString() +" -> "+ it.getValue().resultatTyper()).collect(Collectors.toSet()));
    }


    private static boolean innholderBare(Set<DetaljertResultatType> resultater, DetaljertResultatType detaljertResultatType) {
        return resultater.equals(Collections.singleton(detaljertResultatType));
    }
}
