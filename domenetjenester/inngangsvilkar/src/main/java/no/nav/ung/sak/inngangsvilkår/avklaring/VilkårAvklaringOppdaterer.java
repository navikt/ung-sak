package no.nav.ung.sak.inngangsvilkår.avklaring;

public interface VilkårAvklaringOppdaterer {

    void settAlleAvklaringerTilFerdig(long behandlingId);

    void settAvklartPeriodeUnderArbeidTilIkkeVurdert(long behandlingId);
}
