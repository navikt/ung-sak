package no.nav.ung.sak.formidling.template.dto.innvilgelse;

public record ResultatFlaggDto(
    @Deprecated
    boolean enDagsats,
    @Deprecated
    boolean ettGbeløp,
    boolean lavSats,
    boolean høySats,
    boolean varierendeSats,
    @Deprecated
    boolean oppnårMaksAlder
) {
}
