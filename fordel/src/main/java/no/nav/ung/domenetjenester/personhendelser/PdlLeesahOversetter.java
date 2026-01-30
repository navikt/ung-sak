package no.nav.ung.domenetjenester.personhendelser;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import no.nav.person.pdl.leesah.Endringstype;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon;
import no.nav.ung.domenetjenester.personhendelser.utils.PersonhendelseUtils;
import no.nav.ung.sak.behandlingslager.aktør.PersoninfoBasis;
import no.nav.ung.sak.domene.person.pdl.AktørTjeneste;
import no.nav.ung.sak.domene.person.pdl.PersonBasisTjeneste;
import no.nav.ung.sak.kontrakt.hendelser.DødsfallHendelse;
import no.nav.ung.sak.kontrakt.hendelser.FødselHendelse;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Dependent
public class PdlLeesahOversetter {
    private static final Logger logger = LoggerFactory.getLogger(PdlLeesahOversetter.class);

    private PersonBasisTjeneste personTjeneste;
    private AktørTjeneste aktørTjeneste;

    public PdlLeesahOversetter() {
        // CDI
    }

    @Inject
    public PdlLeesahOversetter(PersonBasisTjeneste personTjeneste, AktørTjeneste aktørTjeneste) {
        this.personTjeneste = personTjeneste;
        this.aktørTjeneste = aktørTjeneste;
    }

    Optional<Hendelse> oversettStøttetPersonhendelse(Personhendelse personhendelse) {
        HendelseInfo hendelseInfo = mapHendelseInfo(personhendelse);
        if (PersonhendelseUtils.gjelderDødsfall(personhendelse)) {
            return oversettDødsfallDersomKomplettData(personhendelse, hendelseInfo);
        } else if (PersonhendelseUtils.gjelderForelderBarnRelasjon(personhendelse)) {
            return oversettForelderBarnRelasjon(personhendelse, hendelseInfo);
        }
        return Optional.empty();
    }

    private HendelseInfo mapHendelseInfo(Personhendelse personhendelse) {
        var builder = new HendelseInfo.Builder()
            .medHendelseId(personhendelse.getHendelseId().toString());
        if (personhendelse.getOpprettet() != null) {
            builder.medOpprettet(LocalDateTime.ofInstant(personhendelse.getOpprettet(), ZoneOffset.systemDefault()));
        }

        personhendelse.getPersonidenter()
            .stream().map(PersonhendelseUtils::mapIdentTilAktørId)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(builder::leggTilAktør);

        return builder.build();
    }

    private Optional<Hendelse> oversettDødsfallDersomKomplettData(Personhendelse personhendelse, HendelseInfo hendelseInfo) {
        if (personhendelse.getDoedsfall() == null || personhendelse.getDoedsfall().getDoedsdato() == null) {
            logger.info("Ignorerer dødsfallhendelse fordi det mangler dødsdato. endringstype {}, hendelseId {}", hendelseInfo.getHendelseId(), personhendelse.getEndringstype());
            // Kan ikke sende inn hendelse til ung-sak dersom den ikke har noen dødsdato - ikke mulig å vurdere periode den gjelder for
            // Denne situasjonen antas bare å oppstå for hendelser med endringstype ANNULLERT, OPPHOERT
            return Optional.empty();
        }

        if (personhendelse.getPersonidenter().isEmpty()) {
            logger.warn("Mottok dødsfallhendelse uten aktørId, gi beskjed til #pdl. HendleseId {}", hendelseInfo.getHendelseId());
        }

        DødsfallHendelse dødsfallHendelse = new DødsfallHendelse.Builder()
            .medHendelseInfo(hendelseInfo)
            .medDødsdato(personhendelse.getDoedsfall().getDoedsdato())
            .build();
        return Optional.of(dødsfallHendelse);
    }

    private Optional<Hendelse> oversettForelderBarnRelasjon(Personhendelse personhendelse, HendelseInfo hendelseInfo) {
        ForelderBarnRelasjon forelderBarnRelasjon = personhendelse.getForelderBarnRelasjon();
        Endringstype endringstype = personhendelse.getEndringstype();

        if (forelderBarnRelasjon == null) {
            logger.info("Ignorerer forelderBarnRelasjon fordi det mangler data. endringstype {}, hendelseId {}", endringstype, hendelseInfo.getHendelseId());
            return Optional.empty();
        }

        if (personhendelse.getPersonidenter().isEmpty()) {
            logger.warn("Mottok forelderBarnRelasjon uten aktørId, gi beskjed til #pdl. HendleseId {}", hendelseInfo.getHendelseId());
        }

        String rolleForPerson = forelderBarnRelasjon.getMinRolleForPerson().toString();

        if (!PersonhendelseUtils.rolleForPersonErForeldre(personhendelse)) {
            logger.info("Ignorerer forelderBarnRelasjon fordi rollen for personen verken er far eller mor. rolleForPerson {}. endringstype {}, hendelseId {}", rolleForPerson, hendelseInfo.getHendelseId(), endringstype);
            return Optional.empty();
        }

        String barnIdent = forelderBarnRelasjon.getRelatertPersonsIdent().toString();
        PersonIdent barnPersonIdent = PersonIdent.fra(barnIdent);
        Optional<AktørId> optionalAktørId = aktørTjeneste.hentAktørIdForPersonIdent(barnPersonIdent);
        if (optionalAktørId.isEmpty()) {
            logger.info("Ignorerer forelderBarnRelasjon fordi det ikke finnes aktørId for barnet. barnIdent {}. endringstype {}, hendelseId {}", barnIdent, endringstype, hendelseInfo.getHendelseId());
            return Optional.empty();
        }

        AktørId barnAktørId = optionalAktørId.get();

        PersoninfoBasis barn = personTjeneste.hentBasisPersoninfo(barnAktørId, barnPersonIdent);
        LocalDate fødselsdato = barn.getFødselsdato();

        if (fødselsdato == null) {
            logger.info("Ignorerer forelderBarnRelasjon fordi det mangler fødselsdato på relasjon. endringstype {}, hendelseId {}", hendelseInfo.getHendelseId(), endringstype);
            // Kan ikke sende inn hendelse dersom den ikke har noen fødselsdato - ikke mulig å vurdere periode den gjelder for
            // Denne situasjonen antas bare å oppstå for hendelser med endringstype ANNULLERT, OPPHOERT
            return Optional.empty();
        }

        logger.info("Oppretter FødselHendelse for {} med barn født {}. hendelseId {}", rolleForPerson, fødselsdato, hendelseInfo.getHendelseId());

        FødselHendelse fødselHendelse = new FødselHendelse.Builder()
            .medHendelseInfo(hendelseInfo)
            .medBarnIdent(barnPersonIdent)
            .medFødselsdato(fødselsdato)
            .build();

        return Optional.of(fødselHendelse);
    }
}
