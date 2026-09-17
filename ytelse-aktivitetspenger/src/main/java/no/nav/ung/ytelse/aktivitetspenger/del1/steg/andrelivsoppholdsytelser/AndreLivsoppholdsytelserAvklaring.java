package no.nav.ung.ytelse.aktivitetspenger.del1.steg.andrelivsoppholdsytelser;

import java.time.LocalDateTime;

public record AndreLivsoppholdsytelserAvklaring(
    AndreLivsoppholdsytelserVarselInnhold innhold,
    String begrunnelse,
    String begrunnelseIkkeVarsel,
    String vurdertAv,
    LocalDateTime vurdertTidspunkt
) {
}
