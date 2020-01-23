package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk;

import no.nav.foreldrepenger.inngangsvilkaar.regelmodell.VilkårGrunnlag;
import no.nav.fpsak.nare.doc.RuleDocumentationGrunnlag;

@RuleDocumentationGrunnlag
public class MedisinskvilkårGrunnlag implements VilkårGrunnlag {

    private final boolean erBehovForPleie;

    public MedisinskvilkårGrunnlag(boolean erBehovForPleie) {
        this.erBehovForPleie = erBehovForPleie;
    }

    public boolean getErBehovForPleie() {
        return erBehovForPleie;
    }

    @Override
    public String toString() {
        return "MedisinskvilkårGrunnlag{}";
    }
}
