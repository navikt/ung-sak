package no.nav.ung.sak.formidling.template;

import no.nav.ung.sak.formidling.template.dto.felles.FellesDto;

/**
 * Object som konverteres til json og mates inn til pdfgen Handlebar
 * Arves av alle som bruker
 */
public interface TemplateData {
    FellesDto felles();
}

