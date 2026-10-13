package no.nav.ung.sak.formidling.template.dto.felles;

import java.util.List;

public record BrevSignaturDto(
    boolean automatiskBehandlet,
    List<BrevAnsvarligDto> ansvarlige) {

    public static BrevSignaturDto automatisk() {
        return new BrevSignaturDto(true, List.of());
    }

}
