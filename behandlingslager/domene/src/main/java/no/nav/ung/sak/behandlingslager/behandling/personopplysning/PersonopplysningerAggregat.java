package no.nav.ung.sak.behandlingslager.behandling.personopplysning;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.threeten.extra.Interval;

import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;

public class PersonopplysningerAggregat {

    private final AktørId søkerAktørId;
    private final List<PersonopplysningEntitet> allePersonopplysninger;
    private final List<PersonRelasjonEntitet> alleRelasjoner;

    public PersonopplysningerAggregat(PersonopplysningGrunnlagEntitet grunnlag, AktørId aktørId, DatoIntervallEntitet forPeriode) {
        this.søkerAktørId = aktørId;
        if (grunnlag.getRegisterVersjon().isPresent()) {
            this.alleRelasjoner = grunnlag.getRegisterVersjon().get().getRelasjoner();
            this.allePersonopplysninger = grunnlag.getRegisterVersjon().get().getPersonopplysninger();
        } else {
            this.alleRelasjoner = Collections.emptyList();
            this.allePersonopplysninger = Collections.emptyList();
        }
    }

    private boolean erGyldigIPeriode(DatoIntervallEntitet forPeriode, DatoIntervallEntitet periode) {
        return periode.tilIntervall().overlaps(forPeriode.tilIntervall());
    }

    private boolean erIkkeSøker(AktørId aktørId, AktørId aktuellAktør) {
        return !aktuellAktør.equals(aktørId);
    }

    public List<PersonopplysningEntitet> getPersonopplysninger() {
        return Collections.unmodifiableList(allePersonopplysninger);
    }

    public PersonopplysningEntitet getPersonopplysning(AktørId aktørId) {
        return allePersonopplysninger.stream().filter(it -> it.getAktørId().equals(aktørId)).findFirst().orElse(null);
    }

    public List<PersonRelasjonEntitet> getRelasjoner() {
        return Collections.unmodifiableList(alleRelasjoner);
    }

    public PersonopplysningEntitet getSøker() {
        return allePersonopplysninger.stream()
            .filter(po -> po.getAktørId().equals(søkerAktørId))
            .findFirst()
            .orElse(null);
    }

    public String getNavn() {
        return getSøker().getNavn();
    }

    public List<PersonopplysningEntitet> getBarna() {
        return getBarnaTil(søkerAktørId);
    }

    public List<PersonopplysningEntitet> getBarnaTil(AktørId aktørId) {
        return getTilPersonerFor(aktørId, RelasjonsRolleType.BARN);
    }

    public Optional<PersonopplysningEntitet> getEktefelle() {
        List<PersonopplysningEntitet> personer = getTilPersonerFor(søkerAktørId, RelasjonsRolleType.EKTE);
        return personer.isEmpty() ? Optional.empty() : Optional.of(personer.get(0));
    }

    public List<PersonRelasjonEntitet> getSøkersRelasjoner() {
        return finnRelasjon(søkerAktørId);
    }

    public Map<AktørId, PersonopplysningEntitet> getAktørPersonopplysningMap() {
        return getPersonopplysninger().stream().collect(Collectors.toMap(PersonopplysningEntitet::getAktørId, Function.identity()));
    }

    public List<PersonopplysningEntitet> getTilPersonerFor(AktørId fraAktørId, RelasjonsRolleType relasjonsRolleType) {
        List<AktørId> tilAktører = alleRelasjoner.stream()
            .filter(e -> e.getRelasjonsrolle().equals(relasjonsRolleType) && e.getAktørId().equals(fraAktørId))
            .map(PersonRelasjonEntitet::getTilAktørId)
            .collect(Collectors.toList());

        List<PersonopplysningEntitet> tilPersoner = new ArrayList<>();
        tilAktører.forEach(e -> {
            allePersonopplysninger.stream()
                .filter(po -> po.getAktørId().equals(e))
                .forEach(p -> tilPersoner.add(p));
        });
        return Collections.unmodifiableList(tilPersoner);
    }

    public Optional<PersonRelasjonEntitet> finnRelasjon(AktørId fraAktørId, AktørId tilAktørId) {
        return getRelasjoner().stream()
            .filter(e -> e.getAktørId().equals(fraAktørId) && e.getTilAktørId().equals(tilAktørId))
            .findFirst();
    }

    public List<PersonRelasjonEntitet> finnRelasjon(AktørId fraAktørId) {
        return getRelasjoner().stream()
            .filter(e -> e.getAktørId().equals(fraAktørId))
            .collect(Collectors.toList());
    }

    public List<PersonopplysningEntitet> getAlleBarnFødtI(Interval fødselIntervall) {
        return getBarnaTil(søkerAktørId).stream()
            .filter(barn -> fødselIntervall.overlaps(byggInterval(barn.getFødselsdato())))
            .collect(Collectors.toList());
    }

    private Interval byggInterval(LocalDate dato) {
        return Interval.of(dato.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant(),
            dato.atStartOfDay().plusDays(1).atZone(ZoneId.systemDefault()).toInstant());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PersonopplysningerAggregat that = (PersonopplysningerAggregat) o;
        return Objects.equals(søkerAktørId, that.søkerAktørId) &&
            Objects.equals(allePersonopplysninger, that.allePersonopplysninger) &&
            Objects.equals(alleRelasjoner, that.alleRelasjoner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(søkerAktørId, allePersonopplysninger, alleRelasjoner);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<" +
            ", allePersonopplysninger=" + allePersonopplysninger +
            '>';
    }
}
