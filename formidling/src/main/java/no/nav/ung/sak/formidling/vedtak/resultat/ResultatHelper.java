package no.nav.ung.sak.formidling.vedtak.resultat;

import java.util.Set;
import java.util.stream.Collectors;

public class ResultatHelper {
    private final Set<DetaljertResultatType> resultatTyper;

    public ResultatHelper(Set<DetaljertResultatInfo> resultatInfo) {
        this.resultatTyper = resultatInfo.stream()
            .map(DetaljertResultatInfo::detaljertResultatType)
            .collect(Collectors.toSet());
    }

    public boolean innholder(DetaljertResultatType detaljertResultatType) {
        return resultatTyper.contains(detaljertResultatType);
    }

    public boolean inneholderBare(DetaljertResultatType detaljertResultatType) {
        return !resultatTyper.isEmpty() && resultatTyper.stream().allMatch(it -> it == detaljertResultatType);
    }
}

