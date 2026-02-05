package no.nav.ung.sak.formidling.innhold;

import no.nav.ung.kodeverk.formidling.TemplateType;

public record TemplateInnholdResultat(
    TemplateType templateType,
    TemplateInnholdDto templateInnholdDto
) {
}
