package no.nav.ung.sak.formidling.template.dto.felles;

import java.time.LocalDate;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.formidling.template.dto.TemplateDto;

/**
 * Felles felter som kan brukes av alle brev. Brukes via {@link TemplateDto}
 */
public record FellesDto(
    LocalDate brevDato,
    MottakerDto mottaker,
    String fagsakYtelse,
    boolean automatiskBehandlet,
    FooterDto footer
) {
    public static FellesDto automatisk(MottakerDto mottakerDto) {
        return new FellesDto(LocalDate.now(),
            mottakerDto,
            FagsakYtelseType.UNGDOMSYTELSE.getKode(),
            true,
            new FooterDto(true));
    }

}
