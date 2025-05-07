package no.nav.ung.kodeverk.behandling.aksjonspunkt;

import no.nav.ung.kodeverk.vilkår.VilkårType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class AksjonspunktKodeDefinisjon {

    // Aksjonspunkt Nr

    public static final String AUTO_MANUELT_SATT_PÅ_VENT_KODE = "7001";
    public static final String AUTO_VENTER_PÅ_KOMPLETT_SØKNAD_KODE = "7003";
    public static final String AUTO_SATT_PÅ_VENT_REVURDERING_KODE = "";
    public static final String AUTO_VENT_PÅ_INNTEKT_RAPPORTERINGSFRIST_KODE = "7014";

    public static final String AUTO_VENT_ETTERLYST_INNTEKTSMELDING_KODE = "7030";
    public static final String AUTO_SATT_PÅ_VENT_ETTERLYST_INNTEKT_UTTALELSE_KODE = "7040";

    public static final String AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE_KODE = "5021";
    public static final String AVKLAR_LOVLIG_OPPHOLD_KODE = "5019";
    public static final String AVKLAR_OM_ER_BOSATT_KODE = "5020";
    public static final String AVKLAR_OPPHOLDSRETT_KODE = "5023";
    public static final String AVKLAR_TILLEGGSOPPLYSNINGER_KODE = "5009";

    public static final String FATTER_VEDTAK_KODE = "5016";

    public static final String FORESLÅ_VEDTAK_KODE = "5015";
    public static final String FORESLÅ_VEDTAK_MANUELT_KODE = "5028";

    public static final String OVERSTYRING_AV_SØKNADSFRISTVILKÅRET_KODE = "6006";

    public static final String OVERSTYRING_AV_INNTEKT_KODE = "6100";


    public static final String SØKERS_OPPLYSNINGSPLIKT_MANU_KODE = "5017";

    public static final String VARSEL_REVURDERING_ETTERKONTROLL_KODE = "5025";
    public static final String VARSEL_REVURDERING_MANUELL_KODE = "5026";
    public static final String KONTROLLER_REVURDERINGSBEHANDLING_VARSEL_VED_UGUNST_KODE = "5055";
    public static final String KONTROLL_AV_MANUELT_OPPRETTET_REVURDERINGSBEHANDLING_KODE = "5056";
    public static final String MANUELL_TILKJENT_YTELSE_KODE = "5057";

    public static final String VEDTAK_UTEN_TOTRINNSKONTROLL_KODE = "5018";

    public static final String VENT_PGA_FOR_TIDLIG_SØKNAD_KODE = "7008";

    public static final String VURDERE_ANNEN_YTELSE_FØR_VEDTAK_KODE = "5033";
    public static final String VURDERE_DOKUMENT_FØR_VEDTAK_KODE = "5034";
    public static final String VURDERE_OVERLAPPENDE_YTELSER_FØR_VEDTAK_KODE = "5040";


    public static final String KONTROLLER_OPPLYSNINGER_OM_SØKNADSFRIST_KODE = "5077";

    public static final String VURDER_FEILUTBETALING_KODE = "5084";
    public static final String SJEKK_TILBAKEKREVING_KODE = "5085";

    public static final String VURDER_TILBAKETREKK_KODE = "5090";


    // Ung
    public static final String KONTROLLER_INNTEKT_KODE = "8000";

    // PSB
    public static final String KONTROLLER_LEGEERKLÆRING_KODE = "9001";

    // Generelt manglende funksjonalitet.
    public static final String AUTO_VENT_FILTER_MANGLENDE_FUNKSJONALITET = "9999";


    static final Map<String, String> KODER;

    static {
        // lag liste av alle koder definert, brukes til konsistensjsekk mot AksjonspunktDefinisjon i tilfelle ubrukte/utgåtte koder.
        var cls = AksjonspunktKodeDefinisjon.class;
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
            return (String) f.get(AksjonspunktKodeDefinisjon.class);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Andre koder, brukes til definisjon av {@link AksjonspunktDefinisjon}.
     */
    public static final SkjermlenkeType UTEN_SKJERMLENKE = null;
    public static final VilkårType UTEN_VILKÅR = null;
    public static final String UTEN_FRIST = null;
    public static final boolean TOTRINN = true;
    public static final boolean ENTRINN = false;

    public static final boolean KAN_OVERSTYRE_TOTRINN_ETTER_LUKKING = true;
    public static final boolean KAN_IKKE_OVERSTYRE_TOTRINN_ETTER_LUKKING = false;
    public static final boolean TILBAKE = true;
    public static final boolean SKAL_IKKE_AVBRYTES = false;
    public static final boolean AVBRYTES = true;
    public static final boolean FORBLI = false;

    public static void main(String[] args) {
        KODER.entrySet().stream().forEach(e -> System.out.println(e));
    }

}
