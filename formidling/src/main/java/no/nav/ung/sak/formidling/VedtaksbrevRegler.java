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
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatInfo;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatType;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultatUtleder;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.VedtaksbrevOperasjonerDto;
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
        return lagResultatMedBygger(detaljertResultatTidslinje);
    }

    private VedtaksbrevRegelResulat lagResultatMedBygger(LocalDateTimeline<DetaljertResultat> detaljertResultat) {
        var resultaterInfo = detaljertResultat
            .toSegments().stream()
            .flatMap(it -> it.getValue().resultatInfo().stream())
            .collect(Collectors.toSet());

        var resultater = resultaterInfo.stream().map(DetaljertResultatInfo::detaljertResultatType).collect(Collectors.toSet());

        if (innholderBare(resultater, DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE)
            || innholderBare(resultater, DetaljertResultatType.INNVILGELSE_UTBETALING_NY_PERIODE, DetaljertResultatType.INNVILGELSE_VILKÅR_NY_PERIODE) ) {
            return new VedtaksbrevRegelResulat(
                VedtaksbrevOperasjonerDto.automatiskBrev("Automatisk brev ved ny innvilgelse"),
                innholdByggere.select(InnvilgelseInnholdBygger.class).get(),
                detaljertResultat
            );
        }

        if (resultater.contains(DetaljertResultatType.KONTROLLER_INNTEKT_REDUKSJON)) {
            return new VedtaksbrevRegelResulat(
                VedtaksbrevOperasjonerDto.automatiskBrev("Automatisk brev ved endring av rapportert inntekt"),
                innholdByggere.select(EndringRapportertInntektInnholdBygger.class).get(),
                detaljertResultat);
        }

        if (innholderBare(resultater, DetaljertResultatType.ENDRING_ØKT_SATS)) {
            return new VedtaksbrevRegelResulat(
                VedtaksbrevOperasjonerDto.automatiskBrev("Automatisk brev ved endring til høy sats"),
                innholdByggere.select(EndringHøySatsInnholdBygger.class).get(),
                detaljertResultat);
        }

        String forklaring = "Ingen brev ved resultater: %s".formatted(String.join(", ", resultaterInfo.stream().map(DetaljertResultatInfo::utledForklaring).toList()));
        return new VedtaksbrevRegelResulat(
            VedtaksbrevOperasjonerDto.ingenBrev(forklaring),
            null,
            detaljertResultat);
    }


    @SafeVarargs
    private static <V> boolean innholderBare(Set<V> set, V... value) {
        return set.equals(Arrays.stream(value).collect(Collectors.toSet()));
    }
}
