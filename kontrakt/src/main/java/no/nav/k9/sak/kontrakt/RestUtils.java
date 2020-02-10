package no.nav.k9.sak.kontrakt;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.ObjectMapper;

class RestUtils {

    public static String convertObjectToQueryString(Object object) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(object, UriFormat.class).toString();
    }

    private static class UriFormat {

        private StringBuilder builder = new StringBuilder();

        @JsonAnySetter
        public void addToUri(String name, Object property) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(name).append("=").append(property);
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
