package no.nav.k9.sak.domene.registerinnhenting;

import java.util.Comparator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;

@ApplicationScoped
@FagsakYtelseTypeRef
public class EndringStartpunktTjeneste {

    private Instance<EndringStartpunktUtleder> utledere;

    EndringStartpunktTjeneste() {
        // For CDI
    }

    @Inject
    public EndringStartpunktTjeneste(@Any Instance<EndringStartpunktUtleder> utledere) {
        this.utledere = utledere;
    }

    public StartpunktType utledStartpunktForDiffBehandlingsgrunnlag(BehandlingReferanse revurdering, EndringsresultatDiff differanse) {
        return differanse.hentKunDelresultater().stream()
            .map(diff -> {
                var utleder = finnUtleder(diff.getGrunnlag(), revurdering.getFagsakYtelseType());
                return utleder.erBehovForStartpunktUtledning(diff) ? utleder.utledStartpunkt(revurdering, diff.getGrunnlagId1(), diff.getGrunnlagId2()) : StartpunktType.UDEFINERT;
            })
            .min(Comparator.comparing(StartpunktType::getRangering))
            .orElse(StartpunktType.UDEFINERT);
    }

    private EndringStartpunktUtleder finnUtleder(Class<?> aggregat, FagsakYtelseType ytelseType) {
        var utleder = EndringStartpunktUtleder.finnUtleder(utledere, aggregat, ytelseType);
        return utleder.orElseThrow(() -> new IllegalArgumentException("Ingen implementasjoner funnet for StartpunktUtleder:" + aggregat + ", ytelseType=" + ytelseType));
    }

}
