package no.nav.k9.sak.domene.registerinnhenting;

import java.util.Comparator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;

@ApplicationScoped
@FagsakYtelseTypeRef
public class StartpunktTjenesteImpl implements StartpunktTjeneste {

    private Instance<StartpunktUtleder> utledere;
    private BehandlingRepository behandlingRepository;

    StartpunktTjenesteImpl() {
        // For CDI
    }

    @Inject
    public StartpunktTjenesteImpl(@Any Instance<StartpunktUtleder> utledere, BehandlingRepositoryProvider provider) {
        this.utledere = utledere;
        this.behandlingRepository = provider.getBehandlingRepository();
    }

    @Override
    public StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, EndringsresultatDiff differanse) {
        var behandling = behandlingRepository.hentBehandling(revurdering.getBehandlingId());
        var fagsakYtelseType = behandling.getFagsakYtelseType();
        var behandlingType = behandling.getType();

        StartpunktType startpunktType = differanse.hentKunDelresultater().stream()
            .map(diff -> {
                var utleder = finnUtleder(diff.getGrunnlag(), fagsakYtelseType, behandlingType);
                return utleder.erBehovForStartpunktUtledning(diff) ? utleder.utledStartpunkt(revurdering, diff.getGrunnlagId1(), diff.getGrunnlagId2()) : StartpunktType.UDEFINERT;
            })
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
        return startpunktType;
    }

    private StartpunktUtleder finnUtleder(Class<?> aggregat, FagsakYtelseType fagsakYtelseType, BehandlingType behandlingType) {
        return GrunnlagRef.Lookup.find(StartpunktUtleder.class, fagsakYtelseType, behandlingType, utledere, aggregat).orElseThrow();
    }

}
