package no.nav.ung.sak.formidling.vedtak.regler;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;

public interface VedtaksbrevRegel {
    BehandlingVedtaksbrevResultat kjør(Long behandlingId);

    static VedtaksbrevRegel hentVedtaksbrevRegel(Instance<VedtaksbrevRegel> regler, BehandlingType behandlingType) {
        return BehandlingTypeRef.Lookup.find(VedtaksbrevRegel.class, regler, FagsakYtelseType.UNGDOMSYTELSE, behandlingType)
            .orElseThrow(() -> new IllegalStateException("Har ikke Vedtaksbrevregel for BehandlingType:" + behandlingType));
    }
}
