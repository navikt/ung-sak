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
    BrevAnsvarligDto brevAnsvarlig) {
    public static FellesDto lag(MottakerDto mottakerDto, BrevAnsvarligDto brevAnsvarlig) {
        return new FellesDto(LocalDate.now(),
            mottakerDto,
            FagsakYtelseType.UNGDOMSYTELSE.getKode(),
            brevAnsvarlig);
    }

}
