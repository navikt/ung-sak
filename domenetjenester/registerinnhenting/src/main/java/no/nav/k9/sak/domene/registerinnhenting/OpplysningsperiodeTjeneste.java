package no.nav.k9.sak.domene.registerinnhenting;

import jakarta.enterprise.inject.Instance;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.typer.Periode;

public interface OpplysningsperiodeTjeneste {

    static OpplysningsperiodeTjeneste getTjeneste(Instance<OpplysningsperiodeTjeneste> instance, FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(instance, ytelseType).orElseThrow(() -> new UnsupportedOperationException("Har ikke " + OpplysningsperiodeTjeneste.class.getSimpleName() + " for " + ytelseType));
    }

    Periode utledOpplysningsperiode(Long behandlingId, boolean tomDagensDato);

    default Periode utledOpplysningsperiodeSkattegrunnlag(Long behandlingId) {
        throw new IllegalStateException("Kan ikke utlede opplysningsperiode for behandling " + behandlingId);
    }

}
