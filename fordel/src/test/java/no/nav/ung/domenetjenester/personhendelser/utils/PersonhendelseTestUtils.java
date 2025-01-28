package no.nav.ung.domenetjenester.personhendelser.utils;

import no.nav.k9.felles.integrasjon.pdl.ForelderBarnRelasjonRolle;
import no.nav.person.pdl.leesah.Endringstype;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.doedsfall.Doedsfall;
import no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static no.nav.ung.domenetjenester.personhendelser.utils.PersonhendelseUtils.DØDSFALL;
import static no.nav.ung.domenetjenester.personhendelser.utils.PersonhendelseUtils.FORELDERBARNRELASJON_V1;

public class PersonhendelseTestUtils {

    public static Personhendelse byggDødsfallHendelse(List<CharSequence> personIdenter) {
        var personhendelse = personhendelseBuilder(personIdenter, DØDSFALL);
        var doedsfall = new Doedsfall();
        doedsfall.setDoedsdato(LocalDate.now());
        personhendelse.setDoedsfall(doedsfall);
        return personhendelse;
    }

    public static Personhendelse byggForelderBarnRelasjonHendelse(List<CharSequence> personIdenter, String relatertPersonIdent, ForelderBarnRelasjonRolle relatertPersonsRolle, ForelderBarnRelasjonRolle foreldersRolle) {
        var personhendelse = personhendelseBuilder(personIdenter, FORELDERBARNRELASJON_V1);
        var forelderBarnRelasjon = new ForelderBarnRelasjon(relatertPersonIdent, relatertPersonsRolle.name(), foreldersRolle.name());
        personhendelse.setForelderBarnRelasjon(forelderBarnRelasjon);
        return personhendelse;
    }


    private static Personhendelse personhendelseBuilder(List<CharSequence> personidenter, String opplysningType) {
        var personhendelse = new Personhendelse();
        personhendelse.setMaster("PDL");
        personhendelse.setOpprettet(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        personhendelse.setHendelseId("123");
        personhendelse.setOpplysningstype(opplysningType);
        personhendelse.setEndringstype(Endringstype.OPPRETTET);
        personhendelse.setPersonidenter(personidenter);
        return personhendelse;
    }
}
