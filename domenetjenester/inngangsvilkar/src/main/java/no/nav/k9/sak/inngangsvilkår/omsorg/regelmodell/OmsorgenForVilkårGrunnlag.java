package no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell;

import java.util.List;
import java.util.Objects;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.VilkårGrunnlag;

public class OmsorgenForVilkårGrunnlag implements VilkårGrunnlag {

    private final Relasjon relasjonMellomSøkerOgPleietrengende;
    private final List<BostedsAdresse> søkersAdresser;
    private final List<BostedsAdresse> pleietrengendeAdresser;
    private final Boolean harBlittVurdertSomOmsorgsPerson;
    private final List<Fosterbarn> fosterbarn;
    private final List<BostedsAdresse> deltBostedsAdresser;
    private LocalDateTimeline<Utfall> knekkpunkter = LocalDateTimeline.empty();

    public OmsorgenForVilkårGrunnlag(Relasjon relasjonMellomSøkerOgPleietrengende, List<BostedsAdresse> søkersAdresser,
                                     List<BostedsAdresse> pleietrengendeAdresser, Boolean harBlittVurdertSomOmsorgsPerson,
                                     List<Fosterbarn> fosterbarn, List<BostedsAdresse> deltBostedsAdresser) {

        this.relasjonMellomSøkerOgPleietrengende = relasjonMellomSøkerOgPleietrengende;
        this.søkersAdresser = søkersAdresser;
        this.pleietrengendeAdresser = pleietrengendeAdresser;
        this.harBlittVurdertSomOmsorgsPerson = harBlittVurdertSomOmsorgsPerson;
        this.fosterbarn = fosterbarn;
        this.deltBostedsAdresser = deltBostedsAdresser;
    }

    public OmsorgenForVilkårGrunnlag(Boolean harBlittVurdertSomOmsorgsPerson) {
        this(null, null, null, harBlittVurdertSomOmsorgsPerson, null, null);
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

    public Boolean getHarBlittVurdertSomOmsorgsPerson() {
        return harBlittVurdertSomOmsorgsPerson;
    }

    public List<Fosterbarn> getFosterbarn() {
        return fosterbarn;
    }

    public List<BostedsAdresse> getDeltBostedsAdresser() {
        return deltBostedsAdresser;
    }

    public void oppdaterKnekkpunkter(OmsorgenForKnekkpunkter omsorgenForKnekkpunkter) {
        if (knekkpunkter == null) {
            throw new IllegalStateException("Kan ikke være null");
        }
        knekkpunkter.compress().forEach(it -> omsorgenForKnekkpunkter.leggTil(DatoIntervallEntitet.fra(it.getLocalDateInterval()), it.getValue()));
    }

    public void leggTilKnekkpunkt(DatoIntervallEntitet periode, Utfall utfall) {
        Objects.requireNonNull(periode);
        Objects.requireNonNull(utfall);
        knekkpunkter = knekkpunkter.combine(new LocalDateSegment<>(periode.toLocalDateInterval(), utfall), StandardCombinators::coalesceRightHandSide, LocalDateTimeline.JoinStyle.CROSS_JOIN);
    }

    @Override
    public String toString() {
        return "OmsorgenForVilkårGrunnlag{" +
            "relasjonMellomSøkerOgPleietrengende=" + relasjonMellomSøkerOgPleietrengende +
            ", søkersAdresser=" + søkersAdresser +
            ", pleietrengendeAdresser=" + pleietrengendeAdresser +
            ", harBlittVurdertSomOmsorgsPerson=" + harBlittVurdertSomOmsorgsPerson +
            ", fosterbarn=" + fosterbarn +
            ", deltBostedsAdresser=" + deltBostedsAdresser +
            '}';
    }
}
