package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.dok;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.arbeidstaker.RegelBeregnBruttoPrArbeidsforhold;
import no.nav.fpsak.nare.doc.RuleDocumentation;

/**
 * Det mangler dokumentasjon
 */

@SuppressWarnings("unchecked")
@RuleDocumentation(value = RegelBeregnBruttoPrArbeidsforhold.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=180066764")
public class DokumentasjonRegelBeregnBruttoPrArbeidsforhold extends RegelBeregnBruttoPrArbeidsforhold implements BeregningsregelDokumentasjon {

    public DokumentasjonRegelBeregnBruttoPrArbeidsforhold() {
        super();
        RegelmodellForDokumentasjon.forArbeidsforhold(this);
    }
}
