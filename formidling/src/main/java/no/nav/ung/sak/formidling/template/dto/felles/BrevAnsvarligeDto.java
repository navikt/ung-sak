package no.nav.ung.sak.formidling.template.dto.felles;

import java.util.List;

public record BrevAnsvarligeDto(
    boolean automatiskBehandlet,
    List<BrevAnsvarligDto> ansvarlige) {

    public static BrevAnsvarligeDto automatisk() {
        return new BrevAnsvarligeDto(true, List.of());
    }

}
