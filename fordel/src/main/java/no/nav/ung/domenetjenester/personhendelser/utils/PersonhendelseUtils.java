package no.nav.ung.domenetjenester.personhendelser.utils;

import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.person.pdl.leesah.forelderbarnrelasjon.ForelderBarnRelasjon;
import no.nav.ung.domenetjenester.personhendelser.AvroJsonUtils;
import no.nav.ung.sak.typer.AktørId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;

public class PersonhendelseUtils {
    private static final Logger logger = LoggerFactory.getLogger(PersonhendelseUtils.class);

    public static final String DØDSFALL = "DOEDSFALL_V1";
    public static final String FORELDERBARNRELASJON_V1 = "FORELDERBARNRELASJON_V1";
    static final Set<String> STØTTEDE_HENDELSE_TYPER = Set.of(DØDSFALL, FORELDERBARNRELASJON_V1);

    static final String FAR = "FAR";
    static final String MOR = "MOR";

    private static final int FNR_LENGTH = 11;

    public static Optional<AktørId> mapIdentTilAktørId(CharSequence personIdent) {

        if (personIdent.toString().length() == FNR_LENGTH) {
            // Bruker kun aktørId, ikke fnr
            if (Environment.current().isDev()) {
                logger.info("Ignorerer personident i hendelse fordi det var 11 sifre, forventet kun aktørId, men fikk: {}", personIdent);
            }
            return Optional.empty();
        } else {
            return Optional.of(new AktørId(personIdent.toString()));
        }
    }

    public static boolean erStøttetHendelseType(Personhendelse personhendelse) {
        return STØTTEDE_HENDELSE_TYPER.contains(personhendelse.getOpplysningstype().toString().trim().toUpperCase());
    }

    public static boolean gjelderDødsfall(Personhendelse personhendelse) {
        return DØDSFALL.contentEquals(personhendelse.getOpplysningstype().toString().trim().toUpperCase());
    }

    public static boolean gjelderForelderBarnRelasjon(Personhendelse personhendelse) {
        return FORELDERBARNRELASJON_V1.contentEquals(personhendelse.getOpplysningstype().toString().trim().toUpperCase());
    }

    public static boolean rolleForPersonErFarEllerMor(Personhendelse personhendelse) {
        ForelderBarnRelasjon forelderBarnRelasjon = personhendelse.getForelderBarnRelasjon();
        CharSequence rolleForPerson = forelderBarnRelasjon.getMinRolleForPerson().toString().trim().toUpperCase();

        return FAR.contentEquals(rolleForPerson) || MOR.contentEquals(rolleForPerson);
    }

    public static String tilJson(Personhendelse personhendelse) {
        return AvroJsonUtils.tilJson(personhendelse);
    }

    public static Personhendelse fraJson(String json) {
        return (Personhendelse) AvroJsonUtils.fraJson(json, Personhendelse.getClassSchema());
    }
}
