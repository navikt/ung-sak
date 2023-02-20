package no.nav.k9.kodeverk.api;

import java.util.Map;

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

    /**
     * Ekstra felter som skal legges på json
     * @return key - value objects
     */
    default Map<String, String> getEkstraFelter(){
        return Map.of();
    }

}
