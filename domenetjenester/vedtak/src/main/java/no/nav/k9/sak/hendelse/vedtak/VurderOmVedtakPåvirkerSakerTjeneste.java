package no.nav.k9.sak.hendelse.vedtak;

import java.util.List;
import java.util.Optional;

import no.nav.abakus.vedtak.ytelse.Ytelse;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

public interface VurderOmVedtakPåvirkerSakerTjeneste {

    static VurderOmVedtakPåvirkerSakerTjeneste finnTjeneste(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(VurderOmVedtakPåvirkerSakerTjeneste.class, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke VurderOmVedtakPåvirkerSakerTjeneste for ytelseType=" + ytelseType));
    }

    static Optional<VurderOmVedtakPåvirkerSakerTjeneste> finnTjenesteHvisStøttet(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(VurderOmVedtakPåvirkerSakerTjeneste.class, ytelseType);
    }

    List<SakMedPeriode> utledSakerMedPerioderSomErKanVærePåvirket(Ytelse vedtakHendelse);
}
