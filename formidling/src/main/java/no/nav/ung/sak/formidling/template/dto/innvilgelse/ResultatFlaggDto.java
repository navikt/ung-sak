package no.nav.ung.sak.formidling.template.dto.innvilgelse;

public record ResultatFlaggDto(
    boolean enDagsats,
    boolean ettGbeløp,
    boolean lavSats,
    boolean høySats,
    boolean varierendeSats,
    boolean oppnårMaksAlder
) {
}
