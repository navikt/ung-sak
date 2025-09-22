package no.nav.ung.sak.behandlingslager.ytelse.sats;

import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;

public enum Sats {

    LAV(UngdomsytelseSatsType.LAV, 18, 25),
    HØY(UngdomsytelseSatsType.HØY, 25, 31);

    private final UngdomsytelseSatsType satsType;
    private final int fomAlder;
    private final int tomAlder;

    Sats(UngdomsytelseSatsType satsType, int fomAlder, int tomAlder) {
        this.satsType = satsType;
        this.fomAlder = fomAlder;
        this.tomAlder = tomAlder;
    }

    public UngdomsytelseSatsType getSatsType() {
        return satsType;
    }

    public int getFomAlder() {
        return fomAlder;
    }

    public int getTilAlder() {
        return tomAlder;
    }
}
