package no.nav.ung.sak.formidling.innhold;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BrevfeilHåndterer {

    private final List<String> feilmeldinger = new ArrayList<>();
    private final boolean kastFeilUmiddelbart;

    public BrevfeilHåndterer(boolean kastFeilUmiddelbart) {
        this.kastFeilUmiddelbart = kastFeilUmiddelbart;
    }

    void registrerFeilmelding(String feilmelding) {
        if (kastFeilUmiddelbart) {
            throw new IllegalStateException(feilmelding);
        }
        feilmeldinger.add(feilmelding);
    }

    public String samletFeiltekst() {
        if (feilmeldinger.isEmpty()) {
            return null;
        }
        return feilmeldinger.stream()
            .map(feil -> "[" + feil + "]")
            .collect(Collectors.joining(", "));


    }

    public boolean harFeil() {
        return !feilmeldinger.isEmpty();
    }
}
