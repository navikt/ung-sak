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
    String fagsakYtelseNavn,
    BrevAnsvarligDto brevAnsvarlig) {

    private static String ytelseNavnBruktIBrev(FagsakYtelseType fagsakYtelseType) {
        return switch (fagsakYtelseType) {
            case UNGDOMSYTELSE -> "ungdomsprogramytelsen";
            case AKTIVITETSPENGER -> "aktivitetspenger";
            default -> fagsakYtelseType.getNavn().toLowerCase();
        };
    }

    public static FellesDto lag(MottakerDto mottakerDto, BrevAnsvarligDto brevAnsvarlig, FagsakYtelseType fagsakYtelseType) {
        return new FellesDto(LocalDate.now(),
            mottakerDto,
            fagsakYtelseType.getKode(),
            ytelseNavnBruktIBrev(fagsakYtelseType),
            brevAnsvarlig);
    }
}
