package no.nav.ung.sak.diff;

import no.nav.ung.kodeverk.api.Kodeverdi;
import no.nav.ung.sak.tid.Range;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.tid.DatoIntervallEntitet;
import no.nav.ung.sak.tid.ÅpenDatoIntervallEntitet;
import no.nav.ung.sak.diff.TraverseGraph;
import no.nav.ung.sak.diff.TraverseGraphConfig;
import no.nav.ung.sak.diff.TraverseJpaEntityGraphConfig;
import no.nav.ung.sak.typer.*;

import java.util.function.Function;

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

        config.addLeafClasses(Beløp.class);
        config.addLeafClasses(AktørId.class);
        config.addLeafClasses(Saksnummer.class);
        config.addLeafClasses(JournalpostId.class);
        config.addLeafClasses(PersonIdent.class);
        config.addLeafClasses(OrgNummer.class);
        config.addLeafClasses(EksternArbeidsforholdRef.class);
        config.addLeafClasses(InternArbeidsforholdRef.class);
        config.addLeafClasses(Stillingsprosent.class);
        config.addLeafClasses(Arbeidsgiver.class);
        config.addLeafClasses(Range.class);

        config.addLeafClasses(DatoIntervallEntitet.class, ÅpenDatoIntervallEntitet.class);
        config.addLeafClasses(Kodeverdi.class);

        config.addLeafClasses(leafClasses);
        return new TraverseGraph(config);
    }

    public static TraverseGraph build() {
        return build(false);
    }
}
