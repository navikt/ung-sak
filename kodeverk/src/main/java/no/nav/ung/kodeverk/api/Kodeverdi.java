package no.nav.ung.kodeverk.api;

/** Kodeverk som er portet til java. */
public interface Kodeverdi extends IndexKey {

    String getKode();

    String getOffisiellKode();

    String getKodeverk();

    String getNavn();

    @Override
    default String getIndexKey() {
        return getKode();
    }

}
