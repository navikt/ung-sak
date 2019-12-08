package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.dok;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.fastsette.refusjon.over6g.RegelFastsettAvkortetVedRefusjonOver6G;
import no.nav.fpsak.nare.doc.RuleDocumentation;

/**
 * Det mangler dokumentasjon
 */

@RuleDocumentation(value = RegelFastsettAvkortetVedRefusjonOver6G.ID, specificationReference = "https://confluence.adeo.no/pages/viewpage.action?pageId=180066764")
public class DokumentasjonRegelFastsettAvkortetVedRefusjonOver6G extends RegelFastsettAvkortetVedRefusjonOver6G implements BeregningsregelDokumentasjon {

    public DokumentasjonRegelFastsettAvkortetVedRefusjonOver6G() {
        super(RegelmodellForDokumentasjon.regelmodellMedEttArbeidsforhold);
    }

}
