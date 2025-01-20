package no.nav.ung.sak.tilgangskontroll.integrasjon.pdl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PipPersondataResponse(String aktoerId, PipPerson person, PipIdenter identer) {

    public String getAktivPersonIdent() {
        List<PipIdent> resultat = identer.identer().stream()
                .filter(ident -> !ident.historisk())
                .filter(ident -> ident.gruppe().equals("FOLKEREGISTERIDENT"))
                .toList();
        if (resultat.size() == 1) {
            return resultat.getFirst().ident();
        }
        throw new IllegalArgumentException("Forventet å finne nøyaktig en aktiv folkeregisterident ident, fant: " + resultat.size());
    }

    public Set<AdressebeskyttelseGradering> getAdressebeskyttelseGradering() {
        return Arrays.stream(person.adressebeskyttelse())
                .map(it -> AdressebeskyttelseGradering.fraKode(it.gradering()))
                .collect(Collectors.toSet());
    }
}
