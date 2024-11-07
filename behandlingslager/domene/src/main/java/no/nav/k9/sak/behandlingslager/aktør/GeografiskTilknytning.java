package no.nav.k9.sak.behandlingslager.aktør;

import no.nav.k9.kodeverk.person.Diskresjonskode;

public class GeografiskTilknytning {
    private final String tilknytning;
    private final Diskresjonskode diskresjonskode;

    public GeografiskTilknytning(String geografiskTilknytning, Diskresjonskode diskresjonskode) {
        this.tilknytning = geografiskTilknytning;
        this.diskresjonskode = diskresjonskode;
    }

    public String getTilknytning() {
        return tilknytning;
    }

    public Diskresjonskode getDiskresjonskode() {
        return diskresjonskode;
    }
}
