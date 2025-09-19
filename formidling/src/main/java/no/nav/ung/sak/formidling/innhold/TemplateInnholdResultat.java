package no.nav.ung.sak.formidling.innhold;

import no.nav.ung.kodeverk.formidling.TemplateType;
import no.nav.ung.sak.formidling.template.dto.TemplateInnholdDto;

public record TemplateInnholdResultat(
    TemplateType templateType,
    TemplateInnholdDto templateInnholdDto,
    boolean automatiskGenerertFooter) {
}
