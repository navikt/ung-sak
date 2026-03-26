package no.nav.ung.ytelse.ungdomsprogramytelsen.registerinnhenting;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.domene.registerinnhenting.EndringStartpunktUtleder;
import no.nav.ung.sak.domene.registerinnhenting.UtvidetRegisterinnhentingTaskUtleder;

import java.util.Collections;
import java.util.List;

@ApplicationScoped
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
public class UngdomsytelseRegisterinnhentingTaskUtleder implements UtvidetRegisterinnhentingTaskUtleder {

    private Instance<EndringStartpunktUtleder> startpunktUtledere;

    UngdomsytelseRegisterinnhentingTaskUtleder() {
    }

    @Inject
    public UngdomsytelseRegisterinnhentingTaskUtleder(Instance<EndringStartpunktUtleder> startpunktUtledere) {
        this.startpunktUtledere = startpunktUtledere;
    }

    @Override
    public List<String> utledRegisterinnhentingTaskTyper(Behandling behandling) {
        if (skalInnhenteProgramperioder(behandling)) {
            return EndringStartpunktUtleder.finnUtleder(startpunktUtledere, UngdomsprogramPeriodeGrunnlag.class, behandling.getFagsakYtelseType())
                .map(_ -> List.of(InnhentUngdomsprogramperioderTask.TASKTYPE))
                .orElse(Collections.emptyList());
        }
        return Collections.emptyList();
    }

    private boolean skalInnhenteProgramperioder(Behandling behandling) {
        if (behandling.getFagsakYtelseType() != FagsakYtelseType.UNGDOMSYTELSE) {
            return false;
        }
        return !behandling.erRevurdering() || BehandlingÅrsakType.årsakerForInnhentingAvProgramperiode().stream().anyMatch(behandling.getBehandlingÅrsakerTyper()::contains);
    }

}
