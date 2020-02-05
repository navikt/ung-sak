package no.nav.foreldrepenger.domene.arbeidsforhold;

import java.util.Optional;

import no.nav.foreldrepenger.behandlingslager.diff.DiffEntity;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraph;
import no.nav.foreldrepenger.behandlingslager.diff.TraverseGraphConfig;
import no.nav.foreldrepenger.domene.typer.tid.DatoIntervallEntitet;
import no.nav.historikk.Saksnummer;
import no.nav.k9.kodeverk.api.Kodeverdi;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Beløp;
import no.nav.k9.sak.typer.JournalpostId;
import no.nav.k9.sak.typer.PersonIdent;

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
        
        config.addLeafClasses(DatoIntervallEntitet.class);
        config.addLeafClasses(Kodeverdi.class);

        this.traverseGraph = new TraverseGraph(config);
        this.diffEntity = new DiffEntity(traverseGraph);
    }
    
    public boolean erForskjellPå(Object object1, Object object2) {
        DiffResult diff = diffEntity.diff(object1, object2);
        return diff.areDifferent();
    }

    public DiffEntity getDiffEntity() {
        return diffEntity;
    }

    public static Optional<Boolean> eksistenssjekkResultat(Optional<?> eksisterende, Optional<?> nytt) {
        if (!eksisterende.isPresent() && !nytt.isPresent()) {
            return Optional.of(Boolean.FALSE);
        }
        if (eksisterende.isPresent() && !nytt.isPresent()) {
            return Optional.of(Boolean.TRUE);
        }
        if (!eksisterende.isPresent() && nytt.isPresent()) { // NOSONAR - "redundant" her er false pos.
            return Optional.of(Boolean.TRUE);
        }
        return Optional.empty();
    }
}
