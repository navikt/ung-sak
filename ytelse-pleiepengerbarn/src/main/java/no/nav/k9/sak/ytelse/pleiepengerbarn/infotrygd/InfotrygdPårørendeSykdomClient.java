package no.nav.k9.sak.ytelse.pleiepengerbarn.infotrygd;

import java.util.List;

interface InfotrygdPårørendeSykdomClient {
    SakResponse getSaker(InfotrygdPårørendeSykdomRequest request);

    List<PårørendeSykdom> getGrunnlagForPleietrengende(InfotrygdPårørendeSykdomRequest request);

    List<VedtakPleietrengende> getVedtakForPleietrengende(InfotrygdPårørendeSykdomRequest request);
}
