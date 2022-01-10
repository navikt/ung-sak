package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import no.nav.k9.sak.typer.PersonIdent;

@Dependent
public class RestInfotrygdPårørendeSykdomService implements InfotrygdPårørendeSykdomService {
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
        var hentGrunnlagRequests = vedtakPleietrengende.stream().map(VedtakPleietrengende::getSoekerFnr).distinct()
            .filter(fnr -> !fnr.equals(ekskludertPersonIdent.getIdent()))
            .map(fnr -> InfotrygdPårørendeSykdomRequest.builder().fødselsnummer(fnr)
                .fraOgMed(request.getFraOgMed())
                .tilOgMed(request.getTilOgMed())
                .relevanteBehandlingstemaer(request.getRelevanteBehandlingstemaer())
                .build())
            .collect(Collectors.toSet());
        var grunnlagsperioderPrIdent = new HashMap<String, List<PeriodeMedBehandlingstema>>();
        for (InfotrygdPårørendeSykdomRequest r : hentGrunnlagRequests) {
            List<PårørendeSykdom> grunnlagliste = client.getGrunnlagForPleietrengende(r);
            var grunnlagsperioder = grunnlagliste.stream()
                .map(PårørendeSykdom::generelt)
                .filter(gr -> erRelevant(gr, request.getRelevanteBehandlingstemaer()))
                .flatMap(gr -> gr.getVedtak().stream().map(v -> new PeriodeMedBehandlingstema(v.periode(), gr.getBehandlingstema().getKode())))
                .collect(Collectors.toList());
            if (!grunnlagliste.isEmpty()) {
                grunnlagsperioderPrIdent.put(r.getFødselsnummer(), grunnlagsperioder);
            }
        }
        return grunnlagsperioderPrIdent;
    }

    private List<VedtakPleietrengende> hentRelevantePleietrengendeVedtakIInfotrygd(InfotrygdPårørendeSykdomRequest request) {
        List<VedtakPleietrengende> response = client.getVedtakForPleietrengende(request);

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


    private boolean erRelevant(GrunnlagPårørendeSykdomInfotrygd grunnlag, Set<String> relevanteBehandlingstemaer) {
        if (grunnlag.getTema() == null || grunnlag.getBehandlingstema() == null) {
            return false;
        }
        if (!Objects.equals(grunnlag.getTema().getKode(), "BS")) {
            return false;
        }
        return relevanteBehandlingstemaer.contains(grunnlag.getBehandlingstema().getKode());
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
