package no.nav.ung.sak.formidling.vedtak.satsendring;

import java.time.LocalDate;

public record SatsEndringUtlederInput(int antallBarn, boolean høySats, long dagsats, long barnetilleggSats, LocalDate fom) {
}

