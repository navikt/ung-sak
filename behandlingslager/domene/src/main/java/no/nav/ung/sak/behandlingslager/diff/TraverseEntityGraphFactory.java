package no.nav.ung.sak.behandlingslager.diff;

import java.util.function.Function;

import no.nav.ung.sak.behandlingslager.Range;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.domene.typer.tid.ÅpenDatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Arbeidsgiver;
import no.nav.ung.sak.typer.Beløp;
import no.nav.ung.sak.typer.EksternArbeidsforholdRef;
import no.nav.ung.sak.typer.InternArbeidsforholdRef;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.OrgNummer;
import no.nav.ung.sak.typer.PersonIdent;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.typer.Stillingsprosent;

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
