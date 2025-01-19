package no.nav.ung.sak.formidling.innhold;

import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.formidling.template.TemplateType;
import no.nav.ung.sak.formidling.template.dto.TemplateInnholdDto;

public record TemplateInnholdResultat(
    DokumentMalType dokumentMalType,
    TemplateType templateType,
    TemplateInnholdDto templateInnholdDto
) {
}
