package no.nav.k9.sak.domene.registerinnhenting;

import java.util.Comparator;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;

@ApplicationScoped
@FagsakYtelseTypeRef
public class StartpunktTjenesteImpl implements StartpunktTjeneste {

    private Instance<StartpunktUtleder> utledere;

    StartpunktTjenesteImpl() {
        // For CDI
    }

    @Inject
    public StartpunktTjenesteImpl(@Any Instance<StartpunktUtleder> utledere) {
        this.utledere = utledere;
    }

    @Override
    public StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, EndringsresultatDiff differanse) {
        return differanse.hentKunDelresultater().stream()
            .map(diff -> {
                var utleder = finnUtleder(diff.getGrunnlag());
                return utleder.erBehovForStartpunktUtledning(diff) ? utleder.utledStartpunkt(revurdering, diff.getGrunnlagId1(), diff.getGrunnlagId2()) : StartpunktType.UDEFINERT;
            })
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private StartpunktUtleder finnUtleder(Class<?> aggregat) {
        return GrunnlagRef.Lookup.find(StartpunktUtleder.class, utledere, aggregat).orElseThrow();
    }

}
