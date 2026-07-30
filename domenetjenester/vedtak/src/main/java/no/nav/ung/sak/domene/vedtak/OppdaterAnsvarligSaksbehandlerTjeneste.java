package no.nav.ung.sak.domene.vedtak;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.kontrakt.aksjonspunkt.BekreftetAksjonspunktDto;

import java.util.Collection;

public interface OppdaterAnsvarligSaksbehandlerTjeneste {

    static OppdaterAnsvarligSaksbehandlerTjeneste finnTjeneste(Instance<OppdaterAnsvarligSaksbehandlerTjeneste> instances, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(OppdaterAnsvarligSaksbehandlerTjeneste.class, instances, ytelseType)
            .orElseThrow(() -> new IllegalStateException("Har ikke tjeneste for ytelseType=" + ytelseType));
    }

    void oppdaterAnsvarligSaksbehandler(Collection<BekreftetAksjonspunktDto> bekreftedeAksjonspunktDtoer, Long behandlingId);

    void oppdaterAnsvarligBeslutter(AksjonspunktDefinisjon fatteVedtakAksjonspunktDefinisjon, Long behandlingId);
}
