package no.nav.ung.sak.formidling.innhold;

import java.util.ArrayList;
import java.util.List;

public class BrevfeilSamler {
    List<String> feilmeldinger = new ArrayList<>();

    void leggTilFeilmelding(String feilmelding) {
        feilmeldinger.add(feilmelding);
    }

    public String joinedFeilmeldinger() {
        if (feilmeldinger.isEmpty()) {
            return null;
        }
        return String.join(", ", feilmeldinger);

    }

    public boolean harFeil() {
        return !feilmeldinger.isEmpty();
    }
}
