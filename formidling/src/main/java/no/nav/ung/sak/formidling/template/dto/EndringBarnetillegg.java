package no.nav.ung.sak.formidling.template.dto;

import java.time.LocalDate;

public record EndringBarnetillegg(
    LocalDate fom,
    long barnetillegg,
    boolean flereBarn

) implements TemplateInnholdDto {
}
