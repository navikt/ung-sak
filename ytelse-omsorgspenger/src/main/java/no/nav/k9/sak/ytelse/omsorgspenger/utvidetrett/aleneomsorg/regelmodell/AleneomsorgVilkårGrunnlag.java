package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.aleneomsorg.regelmodell;

import java.util.List;
import java.util.Map;

import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;
import no.nav.k9.sak.typer.AktørId;

public class AleneomsorgVilkårGrunnlag implements VilkårGrunnlag {

    private final AktørId søkerAktørId;
    private final List<BostedsAdresse> søkerAdresser;
    private final Map<AktørId, List<BostedsAdresse>> foreldreAdresser; //må ha med begge foreldres adresser ettersom søker kan være fosterforelder
    private final List<BostedsAdresse> barnetsDeltBostedAdresser;

    public AleneomsorgVilkårGrunnlag(AktørId søkerAktørId, List<BostedsAdresse> søkerAdresser, Map<AktørId, List<BostedsAdresse>> foreldreAdresser, List<BostedsAdresse> barnetsDeltBostedAdresser) {
        this.søkerAktørId = søkerAktørId;
        this.søkerAdresser = søkerAdresser;
        this.foreldreAdresser = foreldreAdresser;
        this.barnetsDeltBostedAdresser = barnetsDeltBostedAdresser;
    }

    public AktørId getSøkerAktørId() {
        return søkerAktørId;
    }

    public Map<AktørId, List<BostedsAdresse>> getForeldreAdresser() {
        return foreldreAdresser;
    }

    public List<BostedsAdresse> getSøkerAdresser() {
        return søkerAdresser;
    }

    public List<BostedsAdresse> getBarnetsDeltBostedAdresser() {
        return barnetsDeltBostedAdresser;
    }

    @Override
    public String toString() {
        return "AleneOmOmsorgenVilkårGrunnlag{" +
            "søkerAktørId=" + søkerAktørId +
            ", foreldresAdresser=" + foreldreAdresser +
            ", søkersAdresser=" + søkerAdresser +
            ", deltBostedAdresser=" + barnetsDeltBostedAdresser +
            '}';
    }
}
