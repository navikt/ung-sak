package no.nav.ung.sak.formidling.template.dto;

import java.time.LocalDate;

public record EndringBarnetillegg(
    LocalDate fom,
    long nyDagsats,
    long dagsatsBarnetillegg

) implements TemplateInnholdDto {
}
