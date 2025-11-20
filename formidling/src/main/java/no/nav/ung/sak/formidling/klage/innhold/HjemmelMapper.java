package no.nav.ung.sak.formidling.klage.innhold;

import no.nav.ung.sak.behandlingslager.behandling.klage.HjemmelBruktKlagebrev;

import java.util.Collection;

public class HjemmelMapper {

    public static String lagTekst(Collection<HjemmelBruktKlagebrev> paragrafListe, String lovNavn) {
        if (paragrafListe.isEmpty()) {
            return null;
        }
        StringBuilder lovtekst = new StringBuilder(lovNavn + " §");
        if (paragrafListe.size() == 1) {
            return lovtekst.append(" ").append(paragrafListe.iterator().next().getParagrafNummer()).toString();
        }
        lovtekst.append("§ ");
        lovtekst.append(tilSetning(paragrafListe.stream().map(HjemmelBruktKlagebrev::getParagrafNummer).toList()));
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
