package no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell;

import java.util.List;

import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;

public class OmsorgenForVilkårGrunnlag implements VilkårGrunnlag {

    private final Relasjon relasjonMellomSøkerOgPleietrengende;
    private final List<BostedsAdresse> søkersAdresser;
    private final List<BostedsAdresse> pleietrengendeAdresser;
    private final Boolean erOmsorgsPerson;

    public OmsorgenForVilkårGrunnlag(Relasjon relasjonMellomSøkerOgPleietrengende, List<BostedsAdresse> søkersAdresser, List<BostedsAdresse> pleietrengendeAdresser, Boolean erOmsorgsPerson) {

        this.relasjonMellomSøkerOgPleietrengende = relasjonMellomSøkerOgPleietrengende;
        this.søkersAdresser = søkersAdresser;
        this.pleietrengendeAdresser = pleietrengendeAdresser;
        this.erOmsorgsPerson = erOmsorgsPerson;
    }

    public Relasjon getRelasjonMellomSøkerOgPleietrengende() {
        return relasjonMellomSøkerOgPleietrengende;
    }

    public List<BostedsAdresse> getSøkersAdresser() {
        return søkersAdresser;
    }

    public List<BostedsAdresse> getPleietrengendeAdresser() {
        return pleietrengendeAdresser;
    }

    public Boolean getErOmsorgsPerson() {
        return erOmsorgsPerson;
    }

    @Override
    public String toString() {
        return "OmsorgenForGrunnlag{" +
            "erOmsorgsPerson=" + erOmsorgsPerson +
            ", relasjonMellomSøkerOgPleietrengende=" + relasjonMellomSøkerOgPleietrengende +
            ", søkersAdresser=" + søkersAdresser +
            ", pleietrengendeAdresser=" + pleietrengendeAdresser +
            '}';
    }

}
