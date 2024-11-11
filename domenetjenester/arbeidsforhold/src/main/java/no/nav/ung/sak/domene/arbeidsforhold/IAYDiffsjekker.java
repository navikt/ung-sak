package no.nav.ung.sak.domene.arbeidsforhold;

import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.ung.sak.behandlingslager.diff.DiffEntity;
import no.nav.ung.sak.behandlingslager.diff.TraverseGraph;
import no.nav.ung.sak.behandlingslager.diff.TraverseGraphConfig;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
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
