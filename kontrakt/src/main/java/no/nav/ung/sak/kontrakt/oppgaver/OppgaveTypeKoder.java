package no.nav.ung.sak.kontrakt.oppgaver;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktKodeDefinisjon;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class OppgaveTypeKoder {

    public static final String BEKREFT_ENDRET_STARTDATO_KODE = "BEKREFT_ENDRET_STARTDATO";
    public static final String BEKREFT_ENDRET_SLUTTDATO_KODE = "BEKREFT_ENDRET_SLUTTDATO";
    public static final String BEKREFT_ENDRET_PERIODE_KODE = "BEKREFT_ENDRET_PERIODE";
    public static final String BEKREFT_FJERNET_PERIODE_KODE = "BEKREFT_FJERNET_PERIODE";
    public static final String BEKREFT_AVVIK_REGISTERINNTEKT_KODE = "BEKREFT_AVVIK_REGISTERINNTEKT";
    public static final String RAPPORTER_INNTEKT_KODE = "RAPPORTER_INNTEKT";
    public static final String SØK_YTELSE_KODE = "SØK_YTELSE";

    static final Map<String, String> KODER;

    static {
        // lag liste av alle koder definert, brukes til konsistensjsekk mot AksjonspunktDefinisjon i tilfelle ubrukte/utgåtte koder.
        var cls = OppgaveTypeKoder.class;
        var map = new TreeMap<String, String>();
        Arrays.stream(cls.getDeclaredFields())
            .filter(f -> Modifier.isPublic(f.getModifiers()) && f.getType() == String.class && Modifier.isStatic(f.getModifiers()))
            .filter(f -> getValue(f) != null)
            .forEach(f -> {
                var kode = getValue(f);
                // sjekker duplikat kode definisjon
                if (map.putIfAbsent(kode, f.getName()) != null) {
                    throw new IllegalStateException("Duplikat kode for : " + kode);
                }
            });
        KODER = Collections.unmodifiableMap(map);
    }

    private static String getValue(Field f) {
        try {
            return (String) f.get(OppgaveTypeKoder.class);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

}
