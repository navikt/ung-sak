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
    String saksbehandlerNavn,
    String beslutterNavn) {
    public static FellesDto lag(MottakerDto mottakerDto, boolean automatiskBehandletFooter, String saksbehandlerNavn, String beslutterNavn) {
        return new FellesDto(LocalDate.now(),
            mottakerDto,
            FagsakYtelseType.UNGDOMSYTELSE.getKode(),
            automatiskBehandletFooter,
            saksbehandlerNavn,
            beslutterNavn);
    }

}
