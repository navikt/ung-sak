package no.nav.k9.sak.domene.arbeidsforhold;

import no.nav.historikk.Saksnummer;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.sak.behandlingslager.diff.DiffEntity;
import no.nav.k9.sak.behandlingslager.diff.TraverseGraph;
import no.nav.k9.sak.behandlingslager.diff.TraverseGraphConfig;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.*;

public class IAYDiffsjekker {
    private DiffEntity diffEntity;
    private TraverseGraph traverseGraph;

    public IAYDiffsjekker() {
        this(true);
    }

    public IAYDiffsjekker(boolean onlyCheckTrackedFields) {

        var config = new TraverseGraphConfig();
        config.setIgnoreNulls(true);
        config.setOnlyCheckTrackedFields(onlyCheckTrackedFields);
        config.setInclusionFilter(TraverseGraphConfig.NO_FILTER);

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

        config.addLeafClasses(DatoIntervallEntitet.class);
        config.addLeafClasses(Kodeverdi.class);

        this.traverseGraph = new TraverseGraph(config);
        this.diffEntity = new DiffEntity(traverseGraph);
    }

    public DiffEntity getDiffEntity() {
        return diffEntity;
    }

}
