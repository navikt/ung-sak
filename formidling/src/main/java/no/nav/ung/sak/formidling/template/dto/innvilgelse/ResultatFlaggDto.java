package no.nav.ung.sak.formidling.template.dto.innvilgelse;

public record ResultatFlaggDto(
    @Deprecated
    boolean enDagsats,
    @Deprecated
    boolean ettGbeløp,
    boolean kunLavSats,
    boolean kunHøySats,
    boolean varierendeSats,
    @Deprecated
    boolean oppnårMaksAlder,
    boolean harBarn) {
}
