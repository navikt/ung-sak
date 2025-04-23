package no.nav.ung.sak.formidling.template.dto.felles;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.formidling.template.dto.TemplateDto;

import java.time.LocalDate;

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

    public static FellesDto manuell(MottakerDto mottakerDto) {
        return new FellesDto(LocalDate.now(),
            mottakerDto,
            FagsakYtelseType.UNGDOMSYTELSE.getKode(),
            false,
            new FooterDto(true));
    }

}
