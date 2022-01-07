package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.søknadsfrist;

import javax.enterprise.inject.Instance;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

public interface MapTilBrevkode {
    Brevkode getBrevkode();

    static MapTilBrevkode finnBrevkodeMapper(Instance<MapTilBrevkode> brevkodeMappere, FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseTypeRef.Lookup.find(brevkodeMappere, fagsakYtelseType)
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + MapTilBrevkode.class.getSimpleName() + " for ytelseType=" + fagsakYtelseType));
    }
}
