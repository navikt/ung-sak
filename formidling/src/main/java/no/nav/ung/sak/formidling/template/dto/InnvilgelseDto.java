package no.nav.ung.sak.formidling.template.dto;

import no.nav.ung.sak.formidling.template.TemplateData;
import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;

public record InnvilgelseDto(
    FellesDto felles
) implements TemplateData {

}
