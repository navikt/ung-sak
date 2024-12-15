package no.nav.ung.sak.formidling.template;

import no.nav.ung.sak.formidling.template.dto.TemplateData;

/**
 *
 * @param templateType malfilen for pdfgen
 * @param templateData dto for pdfgen
 */
public record TemplateInput(
    TemplateType templateType,
    TemplateData templateData

) {
}
