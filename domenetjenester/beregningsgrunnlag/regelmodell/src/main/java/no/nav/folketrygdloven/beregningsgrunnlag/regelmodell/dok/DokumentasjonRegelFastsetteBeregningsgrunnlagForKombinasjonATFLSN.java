package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.dok;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.kombinasjon.RegelFastsetteBeregningsgrunnlagForKombinasjonATFLSN;
import no.nav.fpsak.nare.doc.RuleDocumentation;

/**
 * Det mangler dokumentasjon
 */

@SuppressWarnings("unchecked")
@RuleDocumentation(value = RegelFastsetteBeregningsgrunnlagForKombinasjonATFLSN.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=216009135")
public class DokumentasjonRegelFastsetteBeregningsgrunnlagForKombinasjonATFLSN
        extends RegelFastsetteBeregningsgrunnlagForKombinasjonATFLSN implements BeregningsregelDokumentasjon {

    public DokumentasjonRegelFastsetteBeregningsgrunnlagForKombinasjonATFLSN() {
        super(RegelmodellForDokumentasjon.regelmodellMedEttArbeidsforhold);
    }

}
