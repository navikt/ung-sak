package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.k9.sak.typer.PersonIdent;

@Dependent
public class RestInfotrygdPårørendeSykdomService implements InfotrygdPårørendeSykdomService {

    private static final Logger log = LoggerFactory.getLogger(RestInfotrygdPårørendeSykdomService.class);


    private InfotrygdPårørendeSykdomClient client;

    public RestInfotrygdPårørendeSykdomService() {
    }

    @Inject
    public RestInfotrygdPårørendeSykdomService(InfotrygdPårørendeSykdomClient client) {
        this.client = client;
    }

    @Override
    public Map<String, List<PeriodeMedBehandlingstema>> hentRelevanteGrunnlagsperioderPrSøkeridentForAndreSøkere(InfotrygdPårørendeSykdomRequest request, PersonIdent ekskludertPersonIdent) {
        var vedtakPleietrengende = hentRelevantePleietrengendeVedtakIInfotrygd(request);
        var fnrSoekere = vedtakPleietrengende.stream().map(VedtakPleietrengende::getSoekerFnr).distinct()
            .filter(fnr -> !fnr.equals(ekskludertPersonIdent.getIdent()))
            .collect(Collectors.toList());
        if (!fnrSoekere.isEmpty()) {
            log.info("Antall andre parter i infotrygd: " + fnrSoekere.size());
        }
        var hentGrunnlagRequest = new PersonRequest(request.getFraOgMed(), request.getTilOgMed(), fnrSoekere);
        List<PårørendeSykdom> grunnlagliste = client.getGrunnlagForPleietrengende(hentGrunnlagRequest);

        var relevanteGrunnlagPrSøker = grunnlagliste.stream()
            .filter(gr -> erRelevant(gr, request.getRelevanteBehandlingstemaer()))
            .collect(Collectors.groupingBy(
                PårørendeSykdom::foedselsnummerSoeker,
                Collectors.flatMapping(mapTilPeriodeMedBehandlingstema(), Collectors.toList())));
        if (!relevanteGrunnlagPrSøker.isEmpty()) {
            log.info("Antall andre parter med relevante grunnlag: " + relevanteGrunnlagPrSøker.keySet().size());
        }
        return relevanteGrunnlagPrSøker;
    }

    private Function<PårørendeSykdom, Stream<PeriodeMedBehandlingstema>> mapTilPeriodeMedBehandlingstema() {
        return gr -> gr.vedtak().stream().map(v -> new PeriodeMedBehandlingstema(v.periode(), gr.behandlingstema().getKode()));
    }

    private List<VedtakPleietrengende> hentRelevantePleietrengendeVedtakIInfotrygd(InfotrygdPårørendeSykdomRequest request) {
        List<VedtakPleietrengende> response = client.getVedtakForPleietrengende(new PersonRequest(request.getFraOgMed(), request.getTilOgMed(), List.of(request.getFødselsnummer())));

        List<VedtakPleietrengende> vedtak = new ArrayList<>();
        for (VedtakPleietrengende vp : response) {
            var relevanteSaker = vp.getVedtak().stream().filter(s -> erRelevant(s, request.getRelevanteBehandlingstemaer()))
                .collect(Collectors.toList());
            if (!relevanteSaker.isEmpty()) {
                var relevantVedtak = new VedtakPleietrengende.Builder().soekerFnr(vp.getSoekerFnr())
                    .vedtak(relevanteSaker)
                    .build();
                vedtak.add(relevantVedtak);
            }
        }
        return vedtak;
    }


    private boolean erRelevant(PårørendeSykdom grunnlag, Set<String> relevanteBehandlingstemaer) {
        if (grunnlag.tema() == null || grunnlag.behandlingstema() == null) {
            return false;
        }
        if (!Objects.equals(grunnlag.tema().getKode(), "BS")) {
            return false;
        }
        return relevanteBehandlingstemaer.contains(grunnlag.behandlingstema().getKode());
    }


    private boolean erRelevant(Sak sak, Set<String> relevanteBehandlingstemaer) {
        if (sak.getTema() == null || sak.getBehandlingstema() == null) {
            return false;
        }

        if (!Objects.equals(sak.getTema().getKode(), "BS")) {
            return false;
        }

        // Ignorer de som har resultat Henlagt/bortfalt & mangler opphørsdato. Da er det aldri gjort noen utbetalinger på dem.
        if (sak.getOpphoerFom() == null && sak.getResultat() != null && Objects.equals(sak.getResultat().getKode(), "HB")) {
            return false;
        }

        return relevanteBehandlingstemaer.contains(sak.getBehandlingstema().getKode());
    }

}
