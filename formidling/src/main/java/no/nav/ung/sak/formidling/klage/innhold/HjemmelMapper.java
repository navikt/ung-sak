package no.nav.ung.sak.formidling.klage.innhold;

import no.nav.ung.kodeverk.hjemmel.KlageHjemmel;

import java.util.Collection;

public class HjemmelMapper {

    public static String lagTekst(Collection<KlageHjemmel> paragrafListe, String lovNavn) {
        if (paragrafListe.isEmpty()) {
            return null;
        }
        StringBuilder lovtekst = new StringBuilder(lovNavn + " §");
        if (paragrafListe.size() == 1) {
            return lovtekst.append(" ").append(paragrafListe.iterator().next().getKode()).toString();
        }
        lovtekst.append("§ ");
        lovtekst.append(tilSetning(paragrafListe.stream().map(KlageHjemmel::getKode).toList()));
        return lovtekst.toString();
    }

    public static String tilSetning(Collection<String> liste) {
        if (liste.size() <= 1) {
            return liste.stream().findFirst().orElse(null);
        }
        String kommaseparertListe = String.join(", ", liste);
        int sisteKommaPosisjon = kommaseparertListe.lastIndexOf(", ");
        return kommaseparertListe.substring(0, sisteKommaPosisjon) + " og" + kommaseparertListe.substring(sisteKommaPosisjon + 1);
    }
}
