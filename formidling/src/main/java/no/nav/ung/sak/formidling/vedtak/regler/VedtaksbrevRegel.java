package no.nav.ung.sak.formidling.vedtak.regler;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;

public interface VedtaksbrevRegel {
    static VedtaksbrevRegel hentVedtaksbrevRegel(FagsakYtelseType ytelseType, Instance<VedtaksbrevRegel> regler, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(VedtaksbrevRegel.class, regler, ytelseType, behandlingType)
            .orElseThrow(() -> new IllegalStateException("Har ikke Vedtaksbrevregel for BehandlingType:" + behandlingType + ", ytelseType:" + ytelseType));
    }

    FellesVedtaksbrevresultat kjør(Long behandlingId);
}
