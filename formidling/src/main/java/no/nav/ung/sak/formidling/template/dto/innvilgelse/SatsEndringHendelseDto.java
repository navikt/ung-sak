package no.nav.ung.sak.formidling.template.dto.innvilgelse;

import java.time.LocalDate;

public record SatsEndringHendelseDto(
    boolean overgangTilHøySats,
    boolean fødselBarn,
    boolean dødsfallBarn,
    LocalDate fom,
    long dagsats,
    int barnetillegg,
    boolean fikkFlereBarn) {

}
