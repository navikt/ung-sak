package no.nav.k9.sak.kontrakt;

import com.fasterxml.jackson.annotation.JsonAnySetter;

public class UriFormat {

    private StringBuilder builder = new StringBuilder();

    public UriFormat() {
        //
    }
    
    public UriFormat(String name, String verdi) {
        addToUri(name, verdi);
    }

    @JsonAnySetter
    public void addToUri(String name, String verdi) {
        if (builder.length() > 0) {
            builder.append("&");
        }
        builder.append(name).append("=").append(verdi);
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}