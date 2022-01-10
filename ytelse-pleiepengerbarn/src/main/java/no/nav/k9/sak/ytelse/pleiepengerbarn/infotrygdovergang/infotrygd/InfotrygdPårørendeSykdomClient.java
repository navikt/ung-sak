package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygdovergang.infotrygd;

import java.util.List;

interface InfotrygdPårørendeSykdomClient {
    List<SakResponse> getSaker(PersonRequest request);

    List<PårørendeSykdom> getGrunnlagForPleietrengende(PersonRequest request);

    List<VedtakPleietrengende> getVedtakForPleietrengende(PersonRequest request);
}
