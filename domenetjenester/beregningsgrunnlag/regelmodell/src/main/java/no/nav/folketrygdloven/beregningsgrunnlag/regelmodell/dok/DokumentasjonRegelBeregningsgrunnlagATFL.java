package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.dok;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.arbeidstaker.RegelBeregningsgrunnlagATFL;
import no.nav.fpsak.nare.doc.RuleDocumentation;

/**
 * Det mangler dokumentasjon
 */

@RuleDocumentation(value = RegelBeregningsgrunnlagATFL.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=180066764")
public class DokumentasjonRegelBeregningsgrunnlagATFL extends RegelBeregningsgrunnlagATFL implements BeregningsregelDokumentasjon {

    public DokumentasjonRegelBeregningsgrunnlagATFL() {
        super(RegelmodellForDokumentasjon.regelmodellMedEttArbeidsforhold);
    }

}
