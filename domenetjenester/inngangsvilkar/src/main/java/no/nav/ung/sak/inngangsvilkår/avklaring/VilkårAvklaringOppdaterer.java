package no.nav.ung.sak.inngangsvilkår.avklaring;

public interface VilkårAvklaringOppdaterer {

    void settAlleAvklaringerTilFerdig(long behandlingId);

    void settVilkårsperioderTilIkkeVurdertForVilkårsavklaringerUnderArbeid(long behandlingId);
}
