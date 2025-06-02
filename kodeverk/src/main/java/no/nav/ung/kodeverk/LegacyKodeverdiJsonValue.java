package no.nav.ung.kodeverk;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Markerer at Kodeverdi klasse skal serialiserast til string med verdi lik kode property.
 * <br>
 * Denne får effekt når LegacyKodeverdiSomObjektSerializer skal serialisere Kodeverdi enum. Den serialiserer i utgangspunktet til objekt,
 * men når denne annotasjonen er satt på klassen serialiserer den til string istadenfor.
 * <br>
 * Brukast på Kodeverdi enums som har hatt @JsonValue annotasjon før LegacyKodeverdiSomObjektSerializer takast i bruk,
 * slik at disse fortsetter å bli serialisert på samme måte som før for legacy klient(er).
 * <br>
 * Fjernast når default LegacyKodeverdiSomObjektSerializer ikkje trengs lenger.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LegacyKodeverdiJsonValue {
}
