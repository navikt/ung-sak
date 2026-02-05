package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.innvilgelse;

import java.time.LocalDate;

public record SatsEndringHendelseDto(
    boolean overgangTilHøySats,
    boolean fødselBarn,
    boolean dødsfallBarn,
    LocalDate fom,
    long dagsats,
    long barnetilleggSats,
    boolean fikkFlereBarn) {

}
