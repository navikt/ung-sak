package no.nav.ung.sak.domene.registerinnhenting;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.typer.Periode;

public interface OpplysningsperiodeTjeneste {

    static OpplysningsperiodeTjeneste getTjeneste(Instance<OpplysningsperiodeTjeneste> instance, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(instance, ytelseType).orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpplysningsperiodeTjeneste.class.getSimpleName() + " for " + ytelseType));
    }

    Periode utledOpplysningsperiode(Long behandlingId, boolean tomDagensDato);

}
