package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;

public record SatsEndring(boolean fødselBarn, boolean dødsfallBarn, boolean fikkFlereBarn,
                          boolean overgangTilHøySats, boolean overgangLavSats) {

    public static SatsEndring bestemSatsendring(UngdomsytelseSatser currentSatser, UngdomsytelseSatser previousSatser) {
        int gjeldendeAntallBarn = currentSatser.antallBarn();
        int tidligereAntallBarn = previousSatser.antallBarn();
        var fødselBarn = gjeldendeAntallBarn > tidligereAntallBarn;
        var dødsfallBarn = gjeldendeAntallBarn < tidligereAntallBarn;
        var fikkFlereBarn = gjeldendeAntallBarn > tidligereAntallBarn && gjeldendeAntallBarn - tidligereAntallBarn > 1;
        var overgangTilHøySats = currentSatser.satsType() == UngdomsytelseSatsType.HØY && previousSatser.satsType() == UngdomsytelseSatsType.LAV;
        var overgangLavSats = currentSatser.satsType() == UngdomsytelseSatsType.LAV && previousSatser.satsType() == UngdomsytelseSatsType.HØY;
        return new SatsEndring(fødselBarn, dødsfallBarn, fikkFlereBarn, overgangTilHøySats, overgangLavSats);
    }
}
