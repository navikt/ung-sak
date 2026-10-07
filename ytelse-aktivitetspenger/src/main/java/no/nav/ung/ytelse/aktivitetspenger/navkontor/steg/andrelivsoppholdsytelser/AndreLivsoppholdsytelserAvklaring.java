package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.andrelivsoppholdsytelser;

import java.time.LocalDateTime;

public record AndreLivsoppholdsytelserAvklaring(
    AndreLivsoppholdsytelserVarselInnhold innhold,
    String begrunnelse,
    String begrunnelseIkkeVarsel,
    String vurdertAv,
    LocalDateTime vurdertTidspunkt
) {
}
