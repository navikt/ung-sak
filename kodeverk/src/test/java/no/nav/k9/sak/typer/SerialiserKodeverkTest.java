package no.nav.k9.sak.typer;

import java.util.EnumMap;
import java.util.Map;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import no.nav.k9.kodeverk.vilkår.VilkårType;

public class SerialiserKodeverkTest {

    private static final ObjectMapper OM = new ObjectMapper();
    
    @Test
    public void skal_serialisere_deserialisere_enum() throws Exception {
        var json = OM.writeValueAsString(VilkårType.OMSORGEN_FOR);
        System.out.println(json);
        
        var vt = OM.readValue(json, VilkårType.class);
        System.out.println(vt);
    }
    
    @Test
    public void skal_serdeser_enum_key_i_map() throws Exception {
        Map<VilkårType, String> map = new EnumMap<>(Map.of(VilkårType.OMSORGEN_FOR, "hello"));
        var json = OM.writeValueAsString(map);
        System.out.println(json);
        
        var vt = OM.readValue(json, Map.class);
        System.out.println(vt);
    }
}
