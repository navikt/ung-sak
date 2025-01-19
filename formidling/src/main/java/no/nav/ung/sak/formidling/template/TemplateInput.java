package no.nav.ung.sak.formidling.template;

import no.nav.ung.sak.formidling.template.dto.TemplateDto;

/**
 *
 * @param templateType    malfilen for pdfgen
 * @param templateDto     dto for pdfgen
 */
public record TemplateInput(TemplateType templateType, TemplateDto templateDto) {
}
