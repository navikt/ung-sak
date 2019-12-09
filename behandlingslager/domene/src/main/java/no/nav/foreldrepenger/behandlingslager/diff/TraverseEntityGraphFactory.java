package no.nav.foreldrepenger.behandlingslager.diff;

import java.util.function.Function;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraph;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraphConfig;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseJpaEntityGraphConfig;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.vedtak.felles.jpa.tid.DatoIntervallEntitet;
import no.nav.vedtak.felles.jpa.tid.ÅpenDatoIntervallEntitet;

public final class TraverseEntityGraphFactory {
    private TraverseEntityGraphFactory() {
    }
    
    public static TraverseGraph build(boolean medChangedTrackedOnly, Class<?>... leafClasses) {
        return build(medChangedTrackedOnly, TraverseGraphConfig.NO_FILTER, leafClasses);
    }

    public static TraverseGraph build(boolean medChangedTrackedOnly, Function<Object, Boolean> inclusionFilter, Class<?>... leafClasses) {
        
        /* default oppsett for behandlingslager. */
        
        var config = new TraverseJpaEntityGraphConfig();
        config.setIgnoreNulls(true);
        config.setOnlyCheckTrackedFields(medChangedTrackedOnly);
        config.addRootClasses(Behandling.class, SøknadEntitet.class);
        config.setInclusionFilter(inclusionFilter);
        
        config.addLeafClasses(DatoIntervallEntitet.class, ÅpenDatoIntervallEntitet.class);
        config.addLeafClasses(Kodeverdi.class);

        config.addLeafClasses(leafClasses);
        return new TraverseGraph(config);
    }

    public static TraverseGraph build() {
        return build(false);
    }
}
