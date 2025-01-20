package no.nav.ung.domenetjenester.personhendelser;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.context.Dependent;
import no.nav.k9.felles.konfigurasjon.env.Environment;
import no.nav.person.pdl.leesah.Personhendelse;
import no.nav.ung.sak.kontrakt.hendelser.DødsfallHendelse;
import no.nav.ung.sak.kontrakt.hendelser.Hendelse;
import no.nav.ung.sak.kontrakt.hendelser.HendelseInfo;
import no.nav.ung.sak.typer.AktørId;

@Dependent
public class PdlLeesahOversetter {

    public static final String DØDSFALL = "DOEDSFALL_V1";

    private static final int FNR_LENGTH = 11;

    private static final Logger logger = LoggerFactory.getLogger(PdlLeesahOversetter.class);

    public PdlLeesahOversetter() {
        // CDI
    }

    Optional<Hendelse> oversettStøttetPersonhendelse(Personhendelse personhendelse) {
        HendelseInfo hendelseInfo = mapHendelseInfo(personhendelse);
        if (DØDSFALL.contentEquals(personhendelse.getOpplysningstype())) {
            return oversettDødsfallDersomKomplettData(personhendelse, hendelseInfo);
        }
        return Optional.empty();
    }

    private HendelseInfo mapHendelseInfo(Personhendelse personhendelse) {
        var builder = new HendelseInfo.Builder()
                .medHendelseId(personhendelse.getHendelseId().toString());
        if (personhendelse.getOpprettet() != null) {
            builder.medOpprettet(LocalDateTime.ofInstant(personhendelse.getOpprettet(), ZoneOffset.systemDefault()));
        }

        for (CharSequence ident : personhendelse.getPersonidenter()) {
            if (ident.toString().length() == FNR_LENGTH) {
                // Bruker kun aktørId, ikke fnr
                if (Environment.current().isDev()) {
                    logger.info("Ignorerer personident i hendelse fordi det var 11 sifre, forventet kun aktørId, men fikk: {}", ident);
                }
                continue;
            }
            builder.leggTilAktør(new AktørId(ident.toString()));
        }
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
}
