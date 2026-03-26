package no.nav.ung.sak.domene.registerinnhenting;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

import java.util.List;
import java.util.Optional;

public interface UtvidetRegisterinnhentingTaskUtleder {

    public static Optional<UtvidetRegisterinnhentingTaskUtleder> finnTjeneste(FagsakYtelseType ytelseType) {
        return FagsakYtelseTypeRef.Lookup.find(UtvidetRegisterinnhentingTaskUtleder.class, ytelseType);
    }


    List<String> utledRegisterinnhentingTaskTyper(Behandling behandling);

}
