package no.nav.ung.sak.formidling.vedtak.resultat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class ResultatHelper {
    private final Set<DetaljertResultatInfo> resultatInfo;
    private final Set<DetaljertResultatType> resultatTyper;

    public ResultatHelper(Set<DetaljertResultatInfo> resultatInfo) {
        this.resultatInfo = resultatInfo;
        this.resultatTyper = resultatInfo.stream()
            .map(DetaljertResultatInfo::detaljertResultatType)
            .collect(Collectors.toSet());
    }

    public boolean innholderBare(DetaljertResultatType... typer) {
        return resultatTyper.equals(Arrays.stream(typer).collect(Collectors.toSet()));
    }

    public ResultatHelper utenom(DetaljertResultatType... typer) {
        var typerSet = Arrays.stream(typer).collect(Collectors.toSet());
        var filtrert = resultatInfo.stream()
            .filter(it -> !typerSet.contains(it.detaljertResultatType()))
            .collect(Collectors.toSet());
        return new ResultatHelper(filtrert);
    }

    public boolean innholder(DetaljertResultatType detaljertResultatType) {
        return resultatTyper.contains(detaljertResultatType);
    }

    public boolean innholderIkke(DetaljertResultatType detaljertResultatType) {
        return !resultatTyper.contains(detaljertResultatType);
    }
}

