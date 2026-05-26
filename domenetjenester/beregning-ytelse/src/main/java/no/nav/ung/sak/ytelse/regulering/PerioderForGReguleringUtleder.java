package no.nav.ung.sak.ytelse.regulering;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.NavigableSet;

public interface PerioderForGReguleringUtleder {


    static PerioderForGReguleringUtleder finnTjeneste(FagsakYtelseType fagsakYtelseType, Instance<PerioderForGReguleringUtleder> tjenester) {
        return FagsakYtelseTypeRef.Lookup.find(tjenester, fagsakYtelseType)
            .orElseThrow(() -> new IllegalStateException("Finner ikke KandidaterForGReguleringTjeneste"));
    }

    NavigableSet<DatoIntervallEntitet> utledPerioderForGRegulering(Behandling behandling, DatoIntervallEntitet periode);

}
