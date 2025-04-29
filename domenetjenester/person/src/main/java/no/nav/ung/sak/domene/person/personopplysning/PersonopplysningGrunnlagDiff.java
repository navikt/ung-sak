package no.nav.ung.sak.domene.person.personopplysning;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.behandlingslager.behandling.RegisterdataDiffsjekker;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonInformasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonRelasjonEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningEntitet;
import no.nav.ung.sak.behandlingslager.behandling.personopplysning.PersonopplysningGrunnlagEntitet;
import no.nav.ung.sak.typer.AktørId;

public class PersonopplysningGrunnlagDiff {
    private AktørId søkerAktørId;
    private PersonopplysningGrunnlagEntitet grunnlag1;
    private PersonopplysningGrunnlagEntitet grunnlag2;
    private Set<AktørId> søkersBarnUnion;
    private Set<AktørId> søkersBarnSnitt;
    private Set<AktørId> søkersBarnDiff;
    private Set<AktørId> personerSnitt;

    public PersonopplysningGrunnlagDiff(AktørId søker, PersonopplysningGrunnlagEntitet grunnlag1, PersonopplysningGrunnlagEntitet grunnlag2) {
        this.søkerAktørId = søker;
        this.grunnlag1 = grunnlag1;
        this.grunnlag2 = grunnlag2;
        søkersBarnUnion = finnAlleBarn();
        søkersBarnSnitt = finnFellesBarn();
        personerSnitt = fellesAktører();
        søkersBarnDiff = søkersBarnUnion.stream().filter(barn -> !søkersBarnSnitt.contains(barn)).collect(Collectors.toSet());
    }

    public boolean erRelasjonerEndret() {
        RegisterdataDiffsjekker differ = new RegisterdataDiffsjekker(true);
        return differ.erForskjellPå(registerVersjon(grunnlag1).map(PersonInformasjonEntitet::getRelasjoner).orElse(Collections.emptyList()),
            registerVersjon(grunnlag2).map(PersonInformasjonEntitet::getRelasjoner).orElse(Collections.emptyList()));
    }

    public boolean erRelasjonerEndretSøkerAntallBarn() {
        return !søkersBarnDiff.isEmpty();
    }

    public boolean erRelasjonerEndretForSøkerUtenomNyeBarn() {
        return erRelasjonerEndretForAktører(Set.of(søkerAktørId), søkersBarnDiff);
    }

    public boolean erRelasjonerEndretForEksisterendeBarn() {
        return erRelasjonerEndretForAktører(søkersBarnSnitt, Collections.emptySet());
    }

    private boolean erRelasjonerEndretForAktører(Set<AktørId> fra, Set<AktørId> ikkeTil) {
        RegisterdataDiffsjekker differ = new RegisterdataDiffsjekker(true);
        return differ.erForskjellPå(hentRelasjonerFraMenIkkeTil(grunnlag1, fra, ikkeTil),
            hentRelasjonerFraMenIkkeTil(grunnlag2, fra, ikkeTil));
    }

    private List<PersonRelasjonEntitet> hentRelasjonerFraMenIkkeTil(PersonopplysningGrunnlagEntitet grunnlag, Set<AktørId> fra, Set<AktørId> ikkeTil) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getRelasjoner).orElse(Collections.emptyList()).stream()
            .filter(rel -> fra.contains(rel.getAktørId()) && !ikkeTil.contains(rel.getTilAktørId()))
            .collect(Collectors.toList());
    }

    public boolean erForeldreDødsdatoEndret() {
        Set<AktørId> foreldre = new HashSet<>();
        foreldre.add(søkerAktørId);
        return !Objects.equals(hentDødsdatoer(grunnlag1, foreldre), hentDødsdatoer(grunnlag2, foreldre));
    }

    public boolean erDødsdatoEndret(AktørId aktørId) {
        return !Objects.equals(hentDødsdatoer(grunnlag1, Set.of(aktørId)), hentDødsdatoer(grunnlag2, Set.of(aktørId)));
    }

    public boolean erBarnDødsdatoEndret() {
        return !Objects.equals(hentDødsdatoer(grunnlag1, søkersBarnUnion), hentDødsdatoer(grunnlag2, søkersBarnUnion));
    }

    private Set<LocalDate> hentDødsdatoer(PersonopplysningGrunnlagEntitet grunnlag, Set<AktørId> aktuelle) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getPersonopplysninger).orElse(Collections.emptyList()).stream()
            .filter(po -> aktuelle.contains(po.getAktørId()))
            .map(PersonopplysningEntitet::getDødsdato)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    private Set<AktørId> finnAlleBarn() {
        Set<AktørId> barn = new HashSet<>();
        barn.addAll(finnBarnaFor(grunnlag1, søkerAktørId));
        barn.addAll(finnBarnaFor(grunnlag2, søkerAktørId));
        return barn;
    }

    private Set<AktørId> finnFellesBarn() {
        Set<AktørId> barn1 = finnBarnaFor(grunnlag1, søkerAktørId);
        Set<AktørId> barn2 = finnBarnaFor(grunnlag2, søkerAktørId);
        return barn2.stream().filter(barn1::contains).collect(Collectors.toSet());
    }

    private Set<AktørId> fellesAktører() {
        Set<AktørId> første = registerVersjon(grunnlag1)
            .map(PersonInformasjonEntitet::getPersonopplysninger)
            .orElse(Collections.emptyList())
            .stream()
            .map(PersonopplysningEntitet::getAktørId)
            .collect(Collectors.toSet());

        return registerVersjon(grunnlag2).map(PersonInformasjonEntitet::getPersonopplysninger).orElse(Collections.emptyList()).stream()
            .map(PersonopplysningEntitet::getAktørId)
            .filter(første::contains)
            .collect(Collectors.toSet());
    }

    private Set<AktørId> finnBarnaFor(PersonopplysningGrunnlagEntitet grunnlag, AktørId forelder) {
        return registerVersjon(grunnlag).map(PersonInformasjonEntitet::getRelasjoner).orElse(Collections.emptyList()).stream()
            .filter(rel -> forelder.equals(rel.getAktørId()))
            .filter(rel -> RelasjonsRolleType.BARN.equals(rel.getRelasjonsrolle()))
            .map(PersonRelasjonEntitet::getTilAktørId)
            .collect(Collectors.toSet());
    }

    private Optional<PersonInformasjonEntitet> registerVersjon(PersonopplysningGrunnlagEntitet grunnlag) {
        return grunnlag != null ? grunnlag.getRegisterVersjon() : Optional.empty();
    }

}
